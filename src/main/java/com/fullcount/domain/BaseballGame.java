package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "baseball_games")
public class BaseballGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 자체 고유 번호 (PK)

    @Column(name = "game_id", unique = true, nullable = false)
    private String gameId; // 네이버 API의 게임 고유 ID (중복 저장 방지용)

    @Column(name = "is_canceled")
    private boolean isCanceled;
    @Column(name = "game_date")
    private String gameDate;
    @Column(name = "game_time")
    private String gameTime;
    @Column(name = "home_team")
    private String homeTeam;
    @Column(name = "away_team")
    private String awayTeam;
    @Column(name = "home_score")
    private Integer homeScore;
    @Column(name = "away_score")
    private Integer awayScore;
    @Column(name = "stadium")
    private String stadium;
    @Column(name = "status")
    private String status;
}
