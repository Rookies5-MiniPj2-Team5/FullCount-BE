package com.fullcount.repository;

import com.fullcount.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}