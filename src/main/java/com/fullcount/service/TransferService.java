package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.PostDto;
import com.fullcount.dto.TransferDto;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.ChatRoomMapper;
import com.fullcount.mapper.PostMapper;
import com.fullcount.mapper.TransferMapper;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public TransferDto.TransferRequestResponse requestTransferByRoomId(Long roomId, Long buyerId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        TicketPost ticketPost = chatRoom.getTicketPost();
        if (ticketPost == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }

        log.info("양도 요청 - roomId={}, ticketPostId={}, buyerId={}", roomId, ticketPost.getId(), buyerId);
        log.info("중복 체크 결과: {}", transferRepository.existsByTicketPostId(ticketPost.getId()));

        if (ticketPost.getAuthor().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.TRANSFER_SELF_NOT_ALLOWED);
        }

        if (transferRepository.existsByTicketPostId(ticketPost.getId())) {
            throw new BusinessException(ErrorCode.TRANSFER_ALREADY_EXISTS);
        }

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Transfer transfer = Transfer.builder()
                .ticketPost(ticketPost)
                .seller(ticketPost.getAuthor())
                .buyer(buyer)
                .price(ticketPost.getPrice())
                .build();
        transferRepository.save(transfer);

        ticketPost.updateStatus(TicketPostStatus.RESERVED);

        return TransferMapper.toTransferRequestResponse(transfer, roomId);
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

        transfer.getSeller().charge(transfer.getPrice());
        transfer.confirmTransfer();

        if (transfer.getPost() != null) {
            transfer.getPost().close();
        } else if (transfer.getTicketPost() != null) {
            transfer.getTicketPost().updateStatus(TicketPostStatus.SOLD);
        }

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

        if (transfer.getStatus() == TransferStatus.COMPLETED ||
                transfer.getStatus() == TransferStatus.CANCELLED) {
            log.warn("거래 취소 불가 - 이미 종료된 거래, transferId={}, status={}", transferId, transfer.getStatus());
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS);
        }

        if (transfer.getStatus() == TransferStatus.TICKET_SENT && isSeller) {
            log.warn("거래 취소 불가 - 티켓 전달 완료 이후 판매자 취소 불가, transferId={}, sellerId={}", transferId, memberId);
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS);
        }

        if (transfer.getStatus() == TransferStatus.PAYMENT_COMPLETED ||
                transfer.getStatus() == TransferStatus.TICKET_SENT) {
            transfer.getBuyer().charge(transfer.getPrice());
            log.info("에스크로 환불 처리 - transferId={}, buyerId={}, price={}",
                    transferId, transfer.getBuyer().getId(), transfer.getPrice());
        }

        Member canceller = isSeller ? transfer.getSeller() : transfer.getBuyer();
        double before = canceller.getMannerTemperature();
        canceller.updateMannerTemperature(-0.1);
        log.info("매너 온도 하락 - memberId={}, {} → {}",
                memberId, before, canceller.getMannerTemperature());

        transfer.cancelTransfer();

        if (transfer.getPost() != null) {
            transfer.getPost().close();
        } else if (transfer.getTicketPost() != null) {
            transfer.getTicketPost().updateStatus(TicketPostStatus.SELLING);
        }

        log.info("거래 취소 완료 - transferId={}, status={}", transferId, transfer.getStatus());
        return TransferMapper.toTransferStatusResponse(transfer);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getMyTransfers(Long memberId, Pageable pageable) {
        Page<PostDto.PostResponse> page = transferRepository.findAllByBuyerId(memberId, pageable)
                .map(transfer -> PostMapper.toResponse(transfer.getPost()));
        return PagedResponse.of(page);
    }

    private Transfer getTransfer(Long transferId) {
        return transferRepository.findByIdWithDetails(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }

    public TransferDto.TransferResponse getTransferByRoomId(Long roomId) {
        try {
            return transferRepository.findByRoomId(roomId)
                    .map(TransferMapper::toTransferResponse)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public TransferDto.TransferRequestResponse requestTransfer(Long postId, Long buyerId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoardType().equals(BoardType.TRANSFER)) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS);
        }
        if (post.getAuthor().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.TRANSFER_SELF_NOT_ALLOWED);
        }
        if (transferRepository.existsByPostId(postId)) {
            throw new BusinessException(ErrorCode.TRANSFER_ALREADY_EXISTS);
        }

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Transfer transfer = TransferMapper.toEntity(post, buyer);
        transferRepository.save(transfer);

        post.reserve();

        Long chatRoomId = chatRoomRepository.findByPostId(postId)
                .map(ChatRoom::getId)
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoomMapper.toEntity(ChatRoomType.ONE_ON_ONE, post, post.getAuthor(), buyer)).getId());

        return TransferMapper.toTransferRequestResponse(transfer, chatRoomId);
    }
}