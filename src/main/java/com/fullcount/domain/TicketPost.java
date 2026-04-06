package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "ticket_post", indexes = {
        @Index(name = "idx_ticket_post_status", columnList = "status"),
        @Index(name = "idx_ticket_post_match_date", columnList = "match_date"),
        @Index(name = "idx_ticket_post_author_id", columnList = "author_id"),
        @Index(name = "idx_ticket_post_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TicketPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "home_team", nullable = false, length = 30)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 30)
    private String awayTeam;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "match_time", nullable = false)
    private LocalTime matchTime;

    @Column(name = "stadium", nullable = false, length = 100)
    private String stadium;

    @Column(name = "seat_area", nullable = false, length = 100)
    private String seatArea;

    @Column(name = "seat_block", length = 50)
    private String seatBlock;

    @Column(name = "seat_row", length = 50)
    private String seatRow;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TicketPostStatus status = TicketPostStatus.SELLING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ────── 비즈니스 메서드 ──────

    public void updateStatus(TicketPostStatus newStatus) {
        this.status = newStatus;
    }

    public void update(String title, String content, String homeTeam, String awayTeam,
                       LocalDate matchDate, LocalTime matchTime, String stadium,
                       String seatArea, String seatBlock, String seatRow, Integer price) {
        this.title = title;
        this.content = content;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.matchDate = matchDate;
        this.matchTime = matchTime;
        this.stadium = stadium;
        this.seatArea = seatArea;
        this.seatBlock = seatBlock;
        this.seatRow = seatRow;
        this.price = price;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.author.getId().equals(memberId);
    }
}
