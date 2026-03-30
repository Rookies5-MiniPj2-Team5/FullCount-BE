package com.fullcount.security;

import com.fullcount.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        try {
            if (StringUtils.hasText(token)) {
                Claims claims = jwtProvider.getClaims(token);

                Long memberId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);

                if (memberId != null && role != null) {
                    setAuthentication(memberId, role);
                }
            }
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            log.warn("JWT 인증 실패: {}", e.getMessage());
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.error("JWT 필터 오류: ", e);
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(Long memberId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                memberId,
                null, // 비밀번호는 필요 없음
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}