package com.ksa.financing.onboarding.foreign.application.dto;

public record ForeignInitiateResponse(
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
