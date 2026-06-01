package com.ksa.financing.onboarding.guest.application.dto;

public record GuestStepResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        boolean onboardingComplete,
        String message,
        String failureReason,
        String timestamp
) {
}
