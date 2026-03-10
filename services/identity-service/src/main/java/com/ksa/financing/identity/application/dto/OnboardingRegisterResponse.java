package com.ksa.financing.identity.application.dto;

public record OnboardingRegisterResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType,
    String keycloakUserId
) {}
