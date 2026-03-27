package com.fullcount.dto;

import com.fullcount.domain.Member;
import lombok.Builder;
import lombok.Getter;

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
    public static class UpdateRequest {
        private String nickname;
    }

    @Getter
    public static class ChangeTeamRequest {
        private Long teamId;
    }
}
