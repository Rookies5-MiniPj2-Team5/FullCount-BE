package com.fullcount.dto;

import com.fullcount.domain.Transfer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class TransferDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long postId;
        private String postTitle;
        private String sellerNickname;
        private String buyerNickname;
        private Integer price;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Transfer t) {
            return Response.builder()
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
    }
}
