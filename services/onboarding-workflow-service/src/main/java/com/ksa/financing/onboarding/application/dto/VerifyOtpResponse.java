package com.ksa.financing.onboarding.application.dto;

import java.util.List;

public record VerifyOtpResponse(
    String workflowId,
    String status,
    String currentStep,
    String nextAction,
    String globalUid,
    String customerId,
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType,
    String failureReason,
    List<StepInfo> steps,
    String timestamp
) {}
