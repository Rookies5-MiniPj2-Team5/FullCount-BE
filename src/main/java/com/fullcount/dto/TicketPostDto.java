package com.fullcount.dto;

import com.fullcount.domain.TicketPost;
import com.fullcount.domain.TicketPostStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TicketPostDto {

    /** 게시글 작성 요청 */
    @Getter
    public static class CreateRequest {

        @NotBlank(message = "제목을 입력해주세요.")
        private String title;

        @NotBlank(message = "본문을 입력해주세요.")
        private String content;

        @NotBlank(message = "홈팀을 입력해주세요.")
        private String homeTeam;

        @NotBlank(message = "어웨이팀을 입력해주세요.")
        private String awayTeam;

        @NotNull(message = "경기 날짜를 입력해주세요.")
        private LocalDate matchDate;

        @NotNull(message = "경기 시간을 입력해주세요.")
        private LocalTime matchTime;

        @NotBlank(message = "경기장을 입력해주세요.")
        private String stadium;

        @NotBlank(message = "좌석 구역을 입력해주세요.")
        private String seatArea;

        private String seatBlock;

        private String seatRow;

        @NotNull(message = "가격을 입력해주세요.")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        private Integer price;
    }

    /** 상태 변경 요청 */
    @Getter
    public static class UpdateStatusRequest {

        @NotNull(message = "변경할 상태를 입력해주세요.")
        private TicketPostStatus status;
    }

    /** 게시글 응답 */
    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private String homeTeam;
        private String awayTeam;
        private LocalDate matchDate;
        private LocalTime matchTime;
        private String stadium;
        private String seatArea;
        private String seatBlock;
        private String seatRow;
        private Integer price;
        private String status;
        private Long authorId;
        private String authorNickname;
        private LocalDateTime createdAt;

        public static Response from(TicketPost ticketPost) {
            return Response.builder()
                    .id(ticketPost.getId())
                    .title(ticketPost.getTitle())
                    .content(ticketPost.getContent())
                    .homeTeam(ticketPost.getHomeTeam())
                    .awayTeam(ticketPost.getAwayTeam())
                    .matchDate(ticketPost.getMatchDate())
                    .matchTime(ticketPost.getMatchTime())
                    .stadium(ticketPost.getStadium())
                    .seatArea(ticketPost.getSeatArea())
                    .seatBlock(ticketPost.getSeatBlock())
                    .seatRow(ticketPost.getSeatRow())
                    .price(ticketPost.getPrice())
                    .status(ticketPost.getStatus().name())
                    .authorId(ticketPost.getAuthor().getId())
                    .authorNickname(ticketPost.getAuthor().getNickname())
                    .createdAt(ticketPost.getCreatedAt())
                    .build();
        }
    }

    /** 티켓 양도 글쓰기 요청 (사용자 요청 스펙) */
    @Getter
    @Setter
    public static class TicketTransferRequestDTO {
        @NotBlank(message = "홈팀을 입력해주세요.")
        private String homeTeam;

        @NotBlank(message = "어웨이팀을 입력해주세요.")
        private String awayTeam;

        @NotNull(message = "경기 날짜를 입력해주세요.")
        private LocalDate matchDate;

        @NotNull(message = "경기 시간을 입력해주세요.")
        private LocalTime matchTime;

        @NotBlank(message = "경기장을 입력해주세요.")
        private String stadium;

        @NotBlank(message = "좌석 구역을 입력해주세요.")
        private String seatArea;

        private String seatBlock;

        private String seatRow;

        @NotNull(message = "가격을 입력해주세요.")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        private Integer price;

        private String description;
    }
}
