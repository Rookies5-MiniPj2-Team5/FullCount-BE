package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false)
    private LocalDateTime expiryAt;

    public void updateToken(String token, LocalDateTime expiryAt) {
        this.token = token;
        this.expiryAt = expiryAt;
    }
}