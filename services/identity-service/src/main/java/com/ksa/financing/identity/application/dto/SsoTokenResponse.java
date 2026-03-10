package com.ksa.financing.identity.application.dto;

import java.util.List;

/**
 * Backend Keycloak se token exchange kar ke yeh frontend ko return karta hai.
 * - accessToken:  Bearer token (har API call mein use hoga)
 * - refreshToken: Token refresh ke liye
 * - expiresIn:    Seconds mein expiry (usually 3600)
 * - tokenType:    Always "Bearer"
 * - roles:        JWT se nikale hue roles (frontend routing ke liye)
 */
public record SsoTokenResponse(
        String accessToken,
        String refreshToken,
        int expiresIn,
        String tokenType,
        List<String> roles,
        java.util.UUID roleId
) {}
