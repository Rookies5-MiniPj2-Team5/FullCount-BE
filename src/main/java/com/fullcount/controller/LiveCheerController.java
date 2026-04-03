package com.fullcount.controller;

import com.fullcount.dto.LiveCheerDTO;
import com.fullcount.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 응원 채팅 컨트롤러
 *
 * 팀원이 담당하는 일반 채팅(/app/chat/{roomId}, /topic/chat/{roomId})과
 * 완전히 독립된 별도 경로를 사용합니다:
 *  - 구독: /topic/live-cheer/{gameId}
 *  - 발행: /app/live-cheer/{gameId}
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LiveCheerController {

    private final SimpMessagingTemplate messagingTemplate;
    private final JwtProvider jwtProvider;

    // 간단한 단어 필터 (실제 운영 시 별도 설정 파일로 분리 권장)
    private static final Set<String> BLOCKED_WORDS = ConcurrentHashMap.newKeySet();

    static {
        BLOCKED_WORDS.add("욕설예시1");
        BLOCKED_WORDS.add("욕설예시2");
    }

    /**
     * 클라이언트 발행: /app/live-cheer/{gameId}
     * 브로드캐스트:   /topic/live-cheer/{gameId}
     *
     * @param gameId  경기 고유 ID (예: "20260403_LG_SSG")
     * @param payload 채팅 또는 리액션 페이로드
     * @param accessor STOMP 헤더 (닉네임, 팀 코드 포함)
     */
    @MessageMapping("/live-cheer/{gameId}")
    public void handleCheer(
            @DestinationVariable String gameId,
            LiveCheerDTO.CheerMessageRequest payload,
            SimpMessageHeaderAccessor accessor
    ) {
        // ── 1. 인증 확인 ─────────────────────────────────────────
        String nickname = (String) accessor.getSessionAttributes().get("nickname");
        String teamCode = (String) accessor.getSessionAttributes().get("teamCode");

        if (nickname == null) {
            log.warn("[LiveCheer] 인증되지 않은 사용자의 메시지 수신 차단. gameId={}", gameId);
            return;
        }

        // ── 2. 욕설 필터 (CHAT 타입만) ──────────────────────────
        String content = payload.getContent();
        if ("CHAT".equals(payload.getType()) && content != null) {
            for (String blocked : BLOCKED_WORDS) {
                content = content.replace(blocked, "***");
            }
        }

        // ── 3. 응답 페이로드 구성 ────────────────────────────────
        LiveCheerDTO.CheerMessageResponse response = LiveCheerDTO.CheerMessageResponse.builder()
                .type(payload.getType())
                .content(content)
                .reactionId(payload.getReactionId())
                .senderNickname(nickname)
                .teamCode(teamCode != null ? teamCode : "")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .build();

        log.info("[LiveCheer] gameId={} | type={} | sender={}", gameId, payload.getType(), nickname);

        // ── 4. 해당 경기의 모든 구독자에게 브로드캐스트 ──────────
        messagingTemplate.convertAndSend("/topic/live-cheer/" + gameId, response);
    }
}
