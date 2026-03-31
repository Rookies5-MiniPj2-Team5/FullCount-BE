package com.fullcount.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BaseballGameDto {
    private String gameId;
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