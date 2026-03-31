package com.fullcount.dto;

import com.fullcount.domain.ChatRoomType;
import lombok.*;

import java.util.List;

public class ChatDTO {

    // ── 메시지 페이로드 ──
    @ToString
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessagePayload {
        private Long messageId;
        private Long senderId;
        private String senderNickname;
        private String content;
        private String timestamp;
    }

    // ── 채팅방 응답 ──
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ChatRoomResponse {
        private Long chatRoomId;
        private ChatRoomType type;
        private String title;
        private String lastMessage;
        private String lastMessageAt;
        private Integer unreadCount;
    }

    // ── 채팅방 상세 ──
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ChatRoomDetailResponse {
        private Long chatRoomId;
        private ChatRoomType type;
        private List<ParticipantResponse> participants;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ParticipantResponse {
        private Long memberId;
        private String nickname;
    }
}
