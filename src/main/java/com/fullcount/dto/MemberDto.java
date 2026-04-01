package com.fullcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberDto {

    @Getter
    @Builder
    public static class MemberResponse {
        private Long id;
        private String email;
        private String nickname;
        private String teamName;
        private String teamShortName;
        private String badgeLevel;
        private Double mannerTemperature;
        private String role;
        private String profileImageUrl;
        private Boolean chatAlert;
        private Boolean transferAlert;
        private Boolean mannerAlert;
    }

    @Getter
    @Builder
    public static class UpdateNickNameResponse {
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @Schema(name = "MemberUpdateRequest", description = "회원 정보 수정 요청")
    public static class UpdateNickNameRequest {
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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 이미지 변경 요청")
    public static class UpdateProfileImageRequest {
        @Schema(description = "이미지 URL 주소")
        private String profileImageUrl;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 변경 요청")
    public static class UpdatePasswordRequest {
        @Schema(description = "현재 비밀번호")
        private String currentPassword;

        @Schema(description = "새 비밀번호")
        private String newPassword;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 설정 변경 요청")
    public static class UpdateAlertRequest {
        private Boolean chatAlert;
        private Boolean transferAlert;
        private Boolean mannerAlert;
    }
}
