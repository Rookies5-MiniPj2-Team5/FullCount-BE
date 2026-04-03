package com.fullcount.config;

import com.fullcount.domain.Member;
import com.fullcount.repository.MemberRepository;
import com.fullcount.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * STOMP CONNECT 프레임에서 JWT를 추출하여 세션 속성에 nickname/teamCode를 저장합니다.
 * 이를 통해 LiveCheerController에서 별도의 DB 조회 없이 발신자 정보를 사용할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[StompAuth] Authorization 헤더 없음 또는 형식이 잘못됨. 비인증 연결로 처리.");
            return message; // 비인증 연결 허용 (읽기 전용으로, 전송 시 차단)
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtProvider.validateToken(token)) {
                log.warn("[StompAuth] 유효하지 않은 JWT. 연결 거부.");
                return message;
            }

            Long memberId = jwtProvider.getMemberId(token);
            Optional<Member> memberOpt = memberRepository.findByIdWithTeam(memberId);

            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                Map<String, Object> attrs = accessor.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("memberId", memberId);
                    attrs.put("nickname", member.getNickname());
                    // 팀 코드: Team.shortName (예: "LG", "DU" 등) — 없으면 빈 문자열
                    String teamCode = (member.getTeam() != null)
                            ? member.getTeam().getShortName()
                            : "";
                    attrs.put("teamCode", teamCode);
                    log.info("[StompAuth] CONNECT 인증 성공: {}, team={}", member.getNickname(), teamCode);
                }
            }
        } catch (Exception e) {
            log.warn("[StompAuth] JWT 처리 중 오류: {}", e.getMessage());
        }

        return message;
    }
}
