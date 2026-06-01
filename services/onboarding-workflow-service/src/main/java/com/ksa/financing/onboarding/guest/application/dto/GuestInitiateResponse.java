package com.ksa.financing.onboarding.guest.application.dto;

public record GuestInitiateResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String mobileOtpRequestId,
        String emailOtpRequestId,
        String maskedMobile,
        String maskedEmail,
        boolean onboardingComplete,
        String failureReason,
        String timestamp
) {
}
