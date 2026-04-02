package com.fullcount.mapper;

import com.fullcount.domain.*;
import com.fullcount.dto.TransferDto;

public class TransferMapper {

    /** 전체 상세 응답 변환 - buyer null 체크 포함 */
    public static TransferDto.TransferResponse toTransferResponse(Transfer t) {
        if (t == null) return null;

        return TransferDto.TransferResponse.builder()
                .id(t.getId())
                .postId(t.getPost().getId())
                .postTitle(t.getPost().getTitle())
                .sellerNickname(t.getSeller().getNickname())
                .buyerNickname(t.getBuyer() != null ? t.getBuyer().getNickname() : null)
                .sellerId(t.getSeller().getId())
                .buyerId(t.getBuyer() != null ? t.getBuyer().getId() : null)  // null 체크 추가
                .price(t.getPrice())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    /** 양도 요청 응답 변환 - chatRoomId 포함 */
    public static TransferDto.TransferRequestResponse toTransferRequestResponse(Transfer t, Long chatRoomId) {
        if (t == null) return null;

        return TransferDto.TransferRequestResponse.builder()
                .transferId(t.getId())
                .chatRoomId(chatRoomId)
                .sellerId(t.getSeller().getId())
                .buyerId(t.getBuyer().getId())
                .build();
    }

    /** 상태 변경 응답 변환 */
    public static TransferDto.TransferStatusResponse toTransferStatusResponse(Transfer t) {
        if (t == null) return null;

        return TransferDto.TransferStatusResponse.builder()
                .status(t.getStatus().name())
                .build();
    }

    /** 양도 요청 Transfer 엔티티 생성 */
    public static Transfer toEntity(Post post, Member buyer) {
        return Transfer.builder()
                .post(post)
                .seller(post.getAuthor())
                .buyer(buyer)
                .price(post.getTicketPrice() != null ? post.getTicketPrice() : 0)
                .build();
    }
}