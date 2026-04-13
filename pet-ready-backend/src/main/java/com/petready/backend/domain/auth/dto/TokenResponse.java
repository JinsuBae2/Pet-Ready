package com.petready.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증 성공 시 발급되는 토큰 세트 응답 DTO입니다.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "토큰 응답")
public class TokenResponse {
    @Schema(description = "Access Token (유효기간 1시간)")
    private String accessToken;

    @Schema(description = "Refresh Token (유효기간 30일)")
    private String refreshToken;
}
