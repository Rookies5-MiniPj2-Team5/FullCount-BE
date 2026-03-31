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

    @Column(unique = true, nullable = false)
    private String gameId; // 네이버 API의 게임 고유 ID (중복 저장 방지용)

    private boolean isCanceled;
    private String gameDate;
    private String gameTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String stadium;
    private String status;
}