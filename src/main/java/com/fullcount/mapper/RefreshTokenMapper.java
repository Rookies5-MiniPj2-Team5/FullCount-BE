package com.fullcount.mapper;

import com.fullcount.domain.RefreshToken;

import java.time.LocalDateTime;

public class RefreshTokenMapper {

    // RefreshToken 생성
    public static RefreshToken toRefreshToken(Long memberId, String token, LocalDateTime expiryAt) {
        return RefreshToken.builder()
                .memberId(memberId)
                .token(token)
                .expiryAt(expiryAt)
                .build();
    }
}