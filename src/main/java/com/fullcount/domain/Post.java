package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "post")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    /** 작성자가 소속된 팀 (팀 전용 게시판 접근 제어용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BoardType boardType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** MEETUP / TRANSFER 에만 필수 */
    private LocalDate matchDate;

    /** TRANSFER 전용 - 티켓 가격 */
    private Integer ticketPrice;

    /** MEETUP 전용 - 최대 모집 인원 */
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PostStatus status = PostStatus.OPEN;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ────── 비즈니스 메서드 ──────

    public void reserve() {
        if (this.status != PostStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태의 게시글만 예약할 수 있습니다.");
        }
        this.status = PostStatus.RESERVED;
    }

    public void close() {
        this.status = PostStatus.CLOSED;
    }

    public boolean isEditable() {
        return this.status == PostStatus.OPEN;
    }

    public void updateContent(String title, String content) {
        if (!isEditable()) {
            throw new IllegalStateException("예약 중이거나 마감된 게시글은 수정할 수 없습니다.");
        }
        this.title = title;
        this.content = content;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
