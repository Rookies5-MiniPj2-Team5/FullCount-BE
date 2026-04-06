package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_participant",
        uniqueConstraints = @UniqueConstraint(name = "uk_crew_participant_post_member", columnNames = {"post_id", "member_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CrewParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "is_leader", nullable = false)
    @Builder.Default
    private Boolean isLeader = false;

    @Column(name = "apply_message", length = 300)
    private String applyMessage;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;

    public void approve() {
        this.isApproved = true;
    }
}
