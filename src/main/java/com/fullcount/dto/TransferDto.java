package com.fullcount.dto;

import com.fullcount.domain.Transfer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class TransferDto {

    @Getter
    @Builder
    public static class TransferResponse {
        private Long id;
        private Long postId;
        private String postTitle;
        private String sellerNickname;
        private String buyerNickname;
        private Integer price;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
