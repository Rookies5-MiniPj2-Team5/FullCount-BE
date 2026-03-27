package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.TransferDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Post post = postRepository.findById(postId)
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

        Transfer transfer = Transfer.builder()
                .post(post)
                .seller(post.getAuthor())
                .buyer(buyer)
                .price(post.getTicketPrice() != null ? post.getTicketPrice() : 0)
                .build();
        transferRepository.save(transfer);

        // 게시글 상태 → 예약 중
        post.reserve();

        // 1:1 채팅방 자동 생성
        if (chatRoomRepository.findByPostId(postId).isEmpty()) {
            ChatRoom chatRoom = ChatRoom.builder()
                    .post(post)
                    .roomType(ChatRoomType.ONE_ON_ONE)
                    .build();
            chatRoomRepository.save(chatRoom);
        }

        return TransferDto.Response.from(transfer);
    }

    /** 에스크로 결제 완료 */
    @Transactional
    public TransferDto.Response payEscrow(Long transferId, Long buyerId) {
        Transfer transfer = getTransfer(transferId);
        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        try {
            transfer.payEscrow(buyer);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS, e.getMessage());
        }
        return TransferDto.Response.from(transfer);
    }

    /** 티켓 전달 완료 (양도자) */
    @Transactional
    public TransferDto.Response markTicketSent(Long transferId, Long sellerId) {
        Transfer transfer = getTransfer(transferId);
        if (!transfer.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        try {
            transfer.markTicketSent();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS, e.getMessage());
        }
        return TransferDto.Response.from(transfer);
    }

    /** 인수 확정 (양수자) */
    @Transactional
    public TransferDto.Response confirmTransfer(Long transferId, Long buyerId) {
        Transfer transfer = getTransfer(transferId);
        if (!transfer.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        try {
            transfer.confirmTransfer();
            transfer.getPost().close();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS, e.getMessage());
        }
        return TransferDto.Response.from(transfer);
    }

    /** 거래 취소 */
    @Transactional
    public TransferDto.Response cancelTransfer(Long transferId, Long memberId) {
        Transfer transfer = getTransfer(transferId);
        boolean isSeller = transfer.getSeller().getId().equals(memberId);
        boolean isBuyer = transfer.getBuyer() != null && transfer.getBuyer().getId().equals(memberId);

        if (!isSeller && !isBuyer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        try {
            transfer.cancelTransfer();
            transfer.getPost().close();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS, e.getMessage());
        }
        return TransferDto.Response.from(transfer);
    }

    private Transfer getTransfer(Long transferId) {
        return transferRepository.findByIdWithDetails(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }
}
