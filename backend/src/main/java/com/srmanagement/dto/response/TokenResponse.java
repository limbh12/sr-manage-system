package com.srmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    /** Access Token */
    private String accessToken;

    /** Refresh Token */
    private String refreshToken;

    /** 토큰 타입 */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Access Token 만료 시간 (밀리초) */
    private Long expiresIn;
}
