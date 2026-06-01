package com.ksa.financing.identity.application.dto;

public record LoginWithEmailPinResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        String keycloakUserId,
        String customerId,
        String pepStatus,
        String name,
        String email,
        String mobileNumber,
        String onboardingFlow,
        boolean onboardingComplete
) {
}
