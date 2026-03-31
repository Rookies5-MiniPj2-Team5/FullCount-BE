package com.fullcount.domain;

import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private BadgeLevel badgeLevel = BadgeLevel.ROOKIE;

    /** 기본 36.5도 */
    @Column(nullable = false)
    @Builder.Default
    private Double mannerTemperature = 36.5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** 시즌 내 팀 변경 여부 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean teamChangedThisSeason = false;

    @Column(nullable = false)
    private int balance = 0;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ────── 비즈니스 메서드 ──────

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 응원 팀 변경 (시즌당 1회 제한) */
    public void changeTeam(Team newTeam) {
        // 1. 이미 이번 시즌에 변경했는지 체크
        if (this.teamChangedThisSeason) {
            throw new BusinessException(ErrorCode.TEAM_CHANGE_LIMIT);
        }

        // 2. 현재 팀과 같은 팀으로 변경하려는지 체크
        if (this.team != null && this.team.getId().equals(newTeam.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        this.team = newTeam;
        this.teamChangedThisSeason = true;
    }

    /** 뱃지 승급 */
    public void upgradeBadge(BadgeLevel newLevel) {
        this.badgeLevel = newLevel;
    }

    /** 매너 온도 갱신 */
    public void updateMannerTemperature(double delta) {
        this.mannerTemperature = Math.min(99.9, Math.max(0.0, this.mannerTemperature + delta));
    }

    /** 시즌 초기화 (매 시즌 시작 시 팀 변경 가능하도록) */
    public void resetSeasonFlags() {
        this.teamChangedThisSeason = false;
    }

    /** 결제 시 충전 */
    public void charge(int amount) {
        this.balance += amount;
    }

    /** 결제 시 차감 */
    public void deduct(int amount) {
        if (this.balance < amount)
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        this.balance -= amount;
    }
}
