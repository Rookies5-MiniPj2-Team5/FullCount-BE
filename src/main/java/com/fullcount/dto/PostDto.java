package com.fullcount.dto;

import com.fullcount.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PostDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "게시글 작성 요청")
    public static class CreatePostRequest {
        @Schema(description = "제목", example = "잠실 직관 가실 분 구합니다")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이내로 작성해주세요.")
        private String title;

        @Schema(description = "내용", example = "3월 30일 잠실 경기 같이 가실 분!")
        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        @Schema(description = "게시판 타입 (TRANSFER, MEETUP, TEAM_ONLY, GENERAL)", example = "TRANSFER")
        @NotNull(message = "게시판 타입을 선택해주세요.")
        private BoardType boardType;

        @Schema(description = "경기 날짜", example = "2026-03-30")
        private LocalDate matchDate;

        @Schema(description = "홈 팀 ID", example = "1")
        private Long homeTeamId;

        @Schema(description = "어웨이 팀 ID", example = "2")
        private Long awayTeamId;

        @Schema(description = "티켓 가격", example = "15000")
        private Integer ticketPrice;

        @Schema(description = "최대 참여 인원", example = "2")
        private Integer maxParticipants;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "게시글 수정 요청")
    public static class UpdatePostRequest {
        @Schema(description = "제목", example = "잠실 경기 동행 모집 수정합니다")
        @NotBlank
        @Size(max = 100)
        private String title;

        @Schema(description = "내용", example = "개인 사정으로 시간을 변경합니다. 오후 6시에 뵈어요.")
        @NotBlank
        private String content;
    }

    @Getter
    @Builder
    public static class PostResponse {
        private Long id;
        private String authorNickname;
        private String teamName;
        private String boardType;
        private String title;
        private String content;
        private LocalDate matchDate;
        private String homeTeamName;
        private String homeTeamId;
        private String awayTeamName;
        private String awayTeamId;
        private Integer ticketPrice;
        private Integer maxParticipants;
        private String status;
        private Integer viewCount;
        private LocalDateTime createdAt;
    }
}
