package com.ksa.financing.onboarding.guest.application.dto;

public record GuestStatusResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String email,
        String mobileNumber,
        String keycloakUserId,
        boolean biometricsEnabled,
        boolean biometricsSkipped,
        boolean pinSet,
        boolean onboardingComplete,
        String failureReason,
        String startedAt,
        String lastUpdatedAt
) {
}
