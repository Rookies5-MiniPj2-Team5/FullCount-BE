package com.fullcount.dto;

import com.fullcount.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String email;
        private String nickname;
        private String teamName;
        private String teamShortName;
        private String badgeLevel;
        private Double mannerTemperature;
        private String role;

        public static Response from(Member member) {
            return Response.builder()
                    .id(member.getId())
                    .email(member.getEmail())
                    .nickname(member.getNickname())
                    .teamName(member.getTeam() != null ? member.getTeam().getName() : null)
                    .teamShortName(member.getTeam() != null ? member.getTeam().getShortName() : null)
                    .badgeLevel(member.getBadgeLevel().name())
                    .mannerTemperature(member.getMannerTemperature())
                    .role(member.getRole().name())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "회원 정보 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "변경할 닉네임", example = "newNickname")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "응원 팀 변경 요청")
    public static class ChangeTeamRequest {
        @Schema(description = "변경할 팀 ID", example = "3")
        private Long teamId;
    }
}
