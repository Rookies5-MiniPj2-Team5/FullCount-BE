package com.fullcount.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    public static class SignupRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 입력해주세요.")
        private String nickname;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        private String password;

        @NotNull(message = "응원 팀을 선택해주세요.")
        private Long teamId;
    }

    @Getter
    @NoArgsConstructor
    public static class LoginRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

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
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType = "Bearer";

        public TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}