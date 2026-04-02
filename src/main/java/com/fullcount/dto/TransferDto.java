package com.fullcount.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class TransferDto {

    /** 양도 요청 응답 (순서 1) - transferId, chatRoomId, sellerId, buyerId 반환 */
    @Getter
    @Builder
    public static class TransferRequestResponse {
        private Long transferId;
        private Long chatRoomId;
        private Long sellerId;
        private Long buyerId;
    }

    /** 상태 변경 응답 (순서 2~4, 취소) - status만 반환 */
    @Getter
    @Builder
    public static class TransferStatusResponse {
        private String status;
    }

    /** 상세 조회용 전체 응답 */
    @Getter
    @Builder
    public static class TransferResponse {
        private Long id;
        private Long postId;
        private String postTitle;
        private String sellerNickname;
        private String buyerNickname;
        private Long sellerId;
        private Long buyerId;
        private Integer price;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}