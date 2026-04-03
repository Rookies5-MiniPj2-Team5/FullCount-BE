package com.fullcount.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fullcount.domain.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    // ==========================================
    // [REQUEST] 게시글 생성 요청 DTO
    // ==========================================

    @Getter
    @NoArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "boardType", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CreateMateRequest.class, name = "MATE"),
            @JsonSubTypes.Type(value = CreateCrewRequest.class, name = "CREW"),
            @JsonSubTypes.Type(value = CreateTransferRequest.class, name = "TRANSFER")
    })
    public abstract static class CreatePostRequest {
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이내로 작성해주세요.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        @NotNull(message = "게시판 타입을 선택해주세요.")
        private BoardType boardType;
    }

    /** 1. 직관 메이트 등록 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateMateRequest extends CreatePostRequest {
        @NotNull(message = "경기 날짜는 필수입니다.")
        @FutureOrPresent(message = "경기 날짜는 과거일 수 없습니다.")
        private LocalDate matchDate;

        @NotNull(message = "홈 팀 ID는 필수입니다.")
        private String homeTeamId;

        @NotNull(message = "어웨이 팀 ID는 필수입니다.")
        private String awayTeamId;

        // 🚨 프론트엔드에서 넘어오는 경기장과 모집 인원 필드 추가
        private String stadium;
        private Integer maxParticipants;
    }

    /** 2. 직관 크루 등록 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCrewRequest extends CreatePostRequest {
        @NotNull(message = "응원팀 설정은 필수입니다.")
        private String supportTeamId;

        @NotNull(message = "모집 인원을 설정해주세요.")
        @Min(value = 2, message = "최소 2명 이상 모집해야 합니다.")
        @Max(value = 50, message = "최대 50명까지 모집 가능합니다.")
        private Integer maxParticipants;

        @Builder.Default
        private Boolean isPublic = true;

        @Size(max = 5, message = "태그는 최대 5개까지만 등록 가능합니다.")
        private List<String> tags;

        @NotBlank(message = "경기장 정보는 필수입니다.")
        private String stadium;

        @NotNull(message = "경기 날짜는 필수입니다.")
        @FutureOrPresent(message = "경기 날짜는 과거일 수 없습니다.")
        private LocalDate matchDate;

        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "시간 형식(HH:mm)이 올바르지 않습니다.")
        private String matchTime;

        private String seatArea;
    }

    /** 3. 티켓 양도 등록 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateTransferRequest extends CreatePostRequest {
        @NotNull(message = "경기 날짜는 필수입니다.")
        private LocalDate matchDate;

        @NotNull(message = "홈 팀 ID는 필수입니다.")
        private String homeTeamId;

        @NotNull(message = "어웨이 팀 ID는 필수입니다.")
        private String awayTeamId;

        private String seatArea;

        @NotNull(message = "티켓 가격을 입력해주세요.")
        @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
        private Integer ticketPrice;
    }

    /** 게시글 수정 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdatePostRequest {
        @NotBlank(message = "제목은 필수입니다.")
        private String title;
        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        // 수정 시에도 경기장과 인원수를 받을 수 있도록 추가
        private String stadium;
        private Integer maxParticipants;
        private String homeTeamId;
        private String awayTeamId;
        private LocalDate matchDate;
    }

    // ==========================================
    // [RESPONSE] 게시글 응답 DTO
    // ==========================================

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "boardType", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MateResponse.class, name = "MATE"),
            @JsonSubTypes.Type(value = CrewResponse.class, name = "CREW"),
            @JsonSubTypes.Type(value = TransferResponse.class, name = "TRANSFER")
    })
    public abstract static class PostResponse {
        private Long id;
        private String authorNickname;
        private String title;
        private String content;
        private BoardType boardType;
        private String status;
        private Integer viewCount;
        private LocalDateTime createdAt;
    }

    @Getter @SuperBuilder @NoArgsConstructor
    public static class MateResponse extends PostResponse {
        private LocalDate matchDate;
        private String homeTeamName;
        private String awayTeamName;
        // 🚨 프론트엔드로 내려줄 경기장, 인원수 필드 추가
        private String stadium;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private String homeTeamId;
        private String awayTeamId;
    }

    @Getter @SuperBuilder @NoArgsConstructor
    public static class CrewResponse extends PostResponse {
        private String supportTeamName;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private String stadium;
        private LocalDate matchDate;
        private String matchTime;
        private String seatArea;
        private List<String> tags;
    }

    @Getter @SuperBuilder @NoArgsConstructor
    public static class TransferResponse extends PostResponse {
        private LocalDate matchDate;
        private String homeTeamName;
        private String awayTeamName;
        private String seatArea;
        private Integer ticketPrice;
    }

    /** 크루 참여 멤버 정보 */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CrewMemberResponse {
        private String nickname;
        private Double mannerTemperature;
        private Boolean isLeader;
    }
}