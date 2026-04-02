package com.fullcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "회원가입 요청")
    public static class SignupRequest {
        @Schema(description = "이메일", example = "testemail@test.com")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @Schema(description = "닉네임", example = "testname")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 입력해주세요.")
        private String nickname;

        @Schema(description = "비밀번호", example = "1q2w3e4r")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        private String password;

        @Schema(description = "응원 팀 ID", example = "1")
        @NotNull(message = "응원 팀을 선택해주세요.")
        private String teamId;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "로그인 요청")
    public static class LoginRequest {
        @Schema(description = "이메일", example = "testemail@test.com")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @Schema(description = "비밀번호", example = "1q2w3e4r")
        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    @Getter
    @NoArgsConstructor
    public static class RefreshTokenRequest {
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType = "Bearer";
    }
}