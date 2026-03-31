package com.fullcount.mapper;

import com.fullcount.domain.*;
import com.fullcount.dto.TransferDto;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {
    public static TransferDto.TransferResponse toTransferResponse(Transfer t) {
        if (t == null) return null;

        return TransferDto.TransferResponse.builder()
                .id(t.getId())
                .postId(t.getPost().getId())
                .postTitle(t.getPost().getTitle())
                .sellerNickname(t.getSeller().getNickname())
                .buyerNickname(t.getBuyer() != null ? t.getBuyer().getNickname() : null)
                .price(t.getPrice())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
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