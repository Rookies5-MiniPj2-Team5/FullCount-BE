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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 잔액 기반 에스크로 결제
 * 추후 토스페이먼츠 또는 카카오페이 연동 예정 */

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;

    /** 양도 요청 + 채팅방 자동 생성 */
    @Transactional
    public TransferDto.Response requestTransfer(Long postId, Long buyerId) {
        // author fetch join으로 N+1 방지
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

        // mapper로 엔티티 생성
        Transfer transfer = TransferMapper.toEntity(post, buyer);
        transferRepository.save(transfer);

        // 게시글 상태 → 예약 중
        post.reserve();

        // 1:1 채팅방 자동 생성
        if (chatRoomRepository.findByPostId(postId).isEmpty()) {
            chatRoomRepository.save(ChatRoomMapper.toEntity(post, ChatRoomType.ONE_ON_ONE));
        }

        return TransferDto.Response.from(transfer);
    }

    /** 에스크로 결제 완료 (잔액 차감) */
    @Transactional
    public TransferDto.Response payEscrow(Long transferId, Long buyerId) {
        Transfer transfer = getTransfer(transferId);
        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!transfer.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 잔액 차감 (부족 시 BusinessException 발생)
        buyer.deduct(transfer.getPrice());

        transfer.payEscrow(buyer);
        return TransferDto.Response.from(transfer);
    }

    /** 티켓 전달 완료 (양도자) */
    @Transactional
    public TransferDto.Response markTicketSent(Long transferId, Long sellerId) {
        Transfer transfer = getTransfer(transferId);

        if (!transfer.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        transfer.markTicketSent();
        return TransferDto.Response.from(transfer);
    }

    /** 인수 확정 (양수자) → 판매자에게 대금 지급 */
    @Transactional
    public TransferDto.Response confirmTransfer(Long transferId, Long buyerId) {
        Transfer transfer = getTransfer(transferId);

        if (!transfer.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 판매자에게 대금 지급
        transfer.getSeller().charge(transfer.getPrice());

        transfer.confirmTransfer();
        transfer.getPost().close();
        return TransferDto.Response.from(transfer);
    }

    /** 거래 취소 (에스크로 결제 이후라면 구매자에게 환불) */
    @Transactional
    public TransferDto.Response cancelTransfer(Long transferId, Long memberId) {
        Transfer transfer = getTransfer(transferId);
        boolean isSeller = transfer.getSeller().getId().equals(memberId);
        boolean isBuyer = transfer.getBuyer() != null && transfer.getBuyer().getId().equals(memberId);

        if (!isSeller && !isBuyer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 에스크로 결제 이후 취소라면 구매자에게 환불
        if (transfer.getStatus() == TransferStatus.PAYMENT_COMPLETED) {
            transfer.getBuyer().charge(transfer.getPrice());
        }

        transfer.cancelTransfer();
        transfer.getPost().close();
        return TransferDto.Response.from(transfer);
    }

    private Transfer getTransfer(Long transferId) {
        return transferRepository.findByIdWithDetails(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }
}