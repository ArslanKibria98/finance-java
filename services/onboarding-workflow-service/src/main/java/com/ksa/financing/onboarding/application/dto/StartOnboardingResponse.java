package com.ksa.financing.onboarding.application.dto;

public record StartOnboardingResponse(
    String workflowId,
    String status,
    String otpRequestId,
    String maskedMobile
) {}
