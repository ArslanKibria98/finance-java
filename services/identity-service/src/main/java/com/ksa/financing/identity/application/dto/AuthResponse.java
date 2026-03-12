package com.ksa.financing.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT authentication response with access and refresh tokens")
public record AuthResponse(
    @Schema(description = "JWT access token for API authorization", example = "eyJhbGciOiJSUzI1NiIs...")
    String accessToken,

    @Schema(description = "Refresh token to obtain new access token", example = "eyJhbGciOiJIUzI1NiIs...")
    String refreshToken,

    @Schema(description = "Access token expiry in seconds", example = "300")
    long expiresIn,

    @Schema(description = "Token type (always Bearer)", example = "Bearer")
    String tokenType,

    @Schema(description = "Customer ID (only present for customer PIN login)", example = "550e8400-e29b-41d4-a716-446655440000")
    String customerId
) {
    /**
     * Constructor without customerId — used by admin login and token refresh.
     */
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, String tokenType) {
        this(accessToken, refreshToken, expiresIn, tokenType, null);
    }
}
