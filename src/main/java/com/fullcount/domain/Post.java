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

    /** MEETUP / CREW 전용 - 응원 팀 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_team_id")
    private Team supportTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BoardType boardType = BoardType.CREW;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** MEETUP / TRANSFER / CREW 에 필수 */
    private LocalDate matchDate;

    /** CREW 전용 - 경기 시간 */
    @Column(length = 20)
    private String matchTime;

    /** CREW 전용 - 경기장 */
    @Column(length = 100)
    private String stadium;

    /** CREW / TRANSFER 전용 - 좌석 구역 */
    @Column(length = 100)
    private String seatArea;

    /** TRANSFER 전용 - 티켓 가격 */
    private Integer ticketPrice;

    /** CREW 전용 - 최대 모집 인원 */
    private Integer maxParticipants;

    /** CREW 전용 - 공개 여부 */
    @Builder.Default
    private Boolean isPublic = true;

    /** CREW 전용 - 태그 (쉼표로 구분하여 저장) */
    @Column(length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.OPEN;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /** 참여자 목록 (CREW 전용) */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<CrewParticipant> participants = new java.util.ArrayList<>();

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

    public void setTeams(Team homeTeam, Team awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public void setSupportTeam(Team supportTeam) {
        this.supportTeam = supportTeam;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }
}
