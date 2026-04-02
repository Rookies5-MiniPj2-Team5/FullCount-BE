package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.TransferDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.ChatRoomMapper;
import com.fullcount.mapper.TransferMapper;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 잔액 기반 에스크로 결제
 * 추후 토스페이먼츠 또는 카카오페이 연동 예정 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;

    /** 양도 요청 + 채팅방 자동 생성 */
    @Transactional
    public TransferDto.TransferRequestResponse requestTransfer(Long postId, Long buyerId) {
        log.info("양도 요청 시작 - postId={}, buyerId={}", postId, buyerId);

        // author fetch join으로 N+1 방지
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoardType().equals(BoardType.TRANSFER)) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS);
        }
        if (post.getAuthor().getId().equals(buyerId)) {
            log.warn("양도 요청 실패 - 자신의 게시글에 양도 요청, postId={}, buyerId={}", postId, buyerId);
            throw new BusinessException(ErrorCode.TRANSFER_SELF_NOT_ALLOWED);
        }
        if (transferRepository.existsByPostId(postId)) {
            log.warn("양도 요청 실패 - 이미 양도 요청 존재, postId={}", postId);
            throw new BusinessException(ErrorCode.TRANSFER_ALREADY_EXISTS);
        }

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // mapper로 엔티티 생성
        Transfer transfer = TransferMapper.toEntity(post, buyer);
        transferRepository.save(transfer);

        // 게시글 상태 → 예약 중
        post.reserve();

        // 1:1 채팅방 자동 생성 후 chatRoomId 추출
        Long chatRoomId = chatRoomRepository.findByPostId(postId)
                .map(ChatRoom::getId)
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoomMapper.toEntity(ChatRoomType.ONE_ON_ONE, post, post.getAuthor(), buyer)).getId());

        log.info("양도 요청 완료 - transferId={}, chatRoomId={}, sellerId={}, buyerId={}",
                transfer.getId(), chatRoomId, post.getAuthor().getId(), buyerId);

        return TransferMapper.toTransferRequestResponse(transfer, chatRoomId);
    }

    /** 에스크로 결제 완료 (잔액 차감) */
    @Transactional
    public TransferDto.TransferStatusResponse payEscrow(Long transferId, Long buyerId) {
        log.info("에스크로 결제 시작 - transferId={}, buyerId={}", transferId, buyerId);

        Transfer transfer = getTransfer(transferId);
        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!transfer.getBuyer().getId().equals(buyerId)) {
            log.warn("에스크로 결제 접근 거부 - transferId={}, buyerId={}", transferId, buyerId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 잔액 차감 (부족 시 BusinessException 발생)
        buyer.deduct(transfer.getPrice());
        transfer.payEscrow(buyer);

        log.info("에스크로 결제 완료 - transferId={}, status={}", transferId, transfer.getStatus());
        return TransferMapper.toTransferStatusResponse(transfer);
    }

    /** 티켓 전달 완료 (양도자) */
    @Transactional
    public TransferDto.TransferStatusResponse markTicketSent(Long transferId, Long sellerId) {
        log.info("티켓 전달 완료 처리 시작 - transferId={}, sellerId={}", transferId, sellerId);

        Transfer transfer = getTransfer(transferId);

        if (!transfer.getSeller().getId().equals(sellerId)) {
            log.warn("티켓 전달 완료 처리 접근 거부 - transferId={}, sellerId={}", transferId, sellerId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        transfer.markTicketSent();

        log.info("티켓 전달 완료 처리 완료 - transferId={}, status={}", transferId, transfer.getStatus());
        return TransferMapper.toTransferStatusResponse(transfer);
    }

    /** 인수 확정 (양수자) → 판매자에게 대금 지급 */
    @Transactional
    public TransferDto.TransferStatusResponse confirmTransfer(Long transferId, Long buyerId) {
        log.info("인수 확정 시작 - transferId={}, buyerId={}", transferId, buyerId);

        Transfer transfer = getTransfer(transferId);

        if (!transfer.getBuyer().getId().equals(buyerId)) {
            log.warn("인수 확정 접근 거부 - transferId={}, buyerId={}", transferId, buyerId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 판매자에게 대금 지급
        transfer.getSeller().charge(transfer.getPrice());
        transfer.confirmTransfer();
        transfer.getPost().close();

        log.info("인수 확정 완료 - transferId={}, status={}", transferId, transfer.getStatus());
        return TransferMapper.toTransferStatusResponse(transfer);
    }

    /** 거래 취소 (에스크로 결제 이후라면 구매자에게 환불) */
    @Transactional
    public TransferDto.TransferStatusResponse cancelTransfer(Long transferId, Long memberId) {
        log.info("거래 취소 시작 - transferId={}, memberId={}", transferId, memberId);

        Transfer transfer = getTransfer(transferId);
        boolean isSeller = transfer.getSeller().getId().equals(memberId);
        boolean isBuyer = transfer.getBuyer() != null && transfer.getBuyer().getId().equals(memberId);

        if (!isSeller && !isBuyer) {
            log.warn("거래 취소 접근 거부 - transferId={}, memberId={}", transferId, memberId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 종료된 거래는 취소 불가
        if (transfer.getStatus() == TransferStatus.COMPLETED ||
                transfer.getStatus() == TransferStatus.CANCELLED) {
            log.warn("거래 취소 불가 - 이미 종료된 거래, transferId={}, status={}", transferId, transfer.getStatus());
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS);
        }

        // 티켓 전달 완료 이후에는 구매자만 취소 가능
        if (transfer.getStatus() == TransferStatus.TICKET_SENT && isSeller) {
            log.warn("거래 취소 불가 - 티켓 전달 완료 이후 판매자 취소 불가, transferId={}, sellerId={}", transferId, memberId);
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS);
        }

        // 에스크로 결제 이후 취소라면 구매자에게 환불
        if (transfer.getStatus() == TransferStatus.PAYMENT_COMPLETED ||
                transfer.getStatus() == TransferStatus.TICKET_SENT) {
            transfer.getBuyer().charge(transfer.getPrice());
            log.info("에스크로 환불 처리 - transferId={}, buyerId={}, price={}",
                    transferId, transfer.getBuyer().getId(), transfer.getPrice());
        }

        // 취소 신청자 매너 온도 -0.1
        Member canceller = isSeller ? transfer.getSeller() : transfer.getBuyer();
        double before = canceller.getMannerTemperature();
        canceller.updateMannerTemperature(-0.1);
        log.info("매너 온도 하락 - memberId={}, {} → {}",
                memberId, before, canceller.getMannerTemperature());

        transfer.cancelTransfer();
        transfer.getPost().close();

        log.info("거래 취소 완료 - transferId={}, status={}", transferId, transfer.getStatus());
        return TransferMapper.toTransferStatusResponse(transfer);
    }

    private Transfer getTransfer(Long transferId) {
        return transferRepository.findByIdWithDetails(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }
}
