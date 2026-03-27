package com.fullcount.dto;

import com.fullcount.domain.*;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PostDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이내로 작성해주세요.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        @NotNull(message = "게시판 타입을 선택해주세요.")
        private BoardType boardType;

        private LocalDate matchDate;
        private Integer ticketPrice;
        private Integer maxParticipants;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        @NotBlank
        @Size(max = 100)
        private String title;

        @NotBlank
        private String content;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String authorNickname;
        private String teamName;
        private String boardType;
        private String title;
        private String content;
        private LocalDate matchDate;
        private Integer ticketPrice;
        private Integer maxParticipants;
        private String status;
        private Integer viewCount;
        private LocalDateTime createdAt;

        public static Response from(Post post) {
            return Response.builder()
                    .id(post.getId())
                    .authorNickname(post.getAuthor().getNickname())
                    .teamName(post.getTeam() != null ? post.getTeam().getName() : null)
                    .boardType(post.getBoardType().name())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .matchDate(post.getMatchDate())
                    .ticketPrice(post.getTicketPrice())
                    .maxParticipants(post.getMaxParticipants())
                    .status(post.getStatus().name())
                    .viewCount(post.getViewCount())
                    .createdAt(post.getCreatedAt())
                    .build();
        }
    }
}
