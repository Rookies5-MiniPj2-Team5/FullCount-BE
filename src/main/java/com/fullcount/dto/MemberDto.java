package com.fullcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
        private Integer balance;
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
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "응원 팀 변경 요청")
    public static class ChangeTeamRequest {
        @Schema(description = "변경할 팀 ID", example = "3")
        @NotBlank(message = "팀 ID는 필수 입력값입니다.")
        @Pattern(regexp = "^[0-9]+$", message = "팀 ID는 숫자만 입력 가능합니다.")
        private String teamId;
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
        @NotBlank(message = "현재 비밀번호는 필수 입력값입니다.")
        private String currentPassword;

        @Schema(description = "새 비밀번호")
        @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]*$",
                message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다."
        )
        private String newPassword;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 설정 변경 요청")
    public static class UpdateAlertRequest {
        @NotNull(message = "채팅 알림 설정값은 필수 입력값입니다.")
        private Boolean chatAlert;

        @NotNull(message = "이적 알림 설정값은 필수 입력값입니다.")
        private Boolean transferAlert;

        @NotNull(message = "매너 알림 설정값은 필수 입력값입니다.")
        private Boolean mannerAlert;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "잔액 변경 요청")
    public static class UpdateBalanceRequest {
        @NotNull(message = "잔액은 필수 입력값입니다.")
        @Min(value = 0, message = "잔액은 0 이상이어야 합니다.")
        @Max(value = 1_000_000_000, message = "잔액은 10억을 초과할 수 없습니다.")
        private Integer balance;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyActivityResponse {
        private Long id;
        private String title;
        private java.time.LocalDateTime createdAt;
        private String status;
    }
}