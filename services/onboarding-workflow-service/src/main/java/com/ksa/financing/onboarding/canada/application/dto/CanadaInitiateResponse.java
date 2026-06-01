package com.ksa.financing.onboarding.canada.application.dto;

public record CanadaInitiateResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String mobileOtpRequestId,
        String emailOtpRequestId,
        String maskedMobile,
        String maskedEmail,
        String failureReason,
        String timestamp
) {
}
