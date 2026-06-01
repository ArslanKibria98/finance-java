package com.ksa.financing.onboarding.guest.application.dto;

public record GuestVerifyOtpResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String keycloakUserId,
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        boolean onboardingComplete,
        String failureReason,
        String timestamp
) {
}
