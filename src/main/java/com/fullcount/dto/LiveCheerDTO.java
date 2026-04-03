package com.fullcount.dto;

import lombok.*;

public class LiveCheerDTO {

    /** 클라이언트 → 서버 (발행 페이로드) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheerMessageRequest {
        /** "CHAT" | "REACTION" */
        private String type;
        /** 채팅 메시지 내용 (type=CHAT 일 때 사용) */
        private String content;
        /** 리액션 이모지 ID (type=REACTION 일 때 사용, 예: "homerun", "cheer", "strike") */
        private String reactionId;
    }

    /** 서버 → 클라이언트 (브로드캐스트 페이로드) */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CheerMessageResponse {
        /** "CHAT" | "REACTION" | "SYSTEM" */
        private String type;
        private String content;
        private String reactionId;
        private String senderNickname;
        private String teamCode;  // 응원팀 코드 (프론트 팀 색상 적용용)
        private String timestamp;
    }
}
