package com.ksa.financing.onboarding.application.dto;

import java.util.List;

public record InitiateOnboardingResponse(
    String workflowId,
    String status,
    String currentStep,
    String nextAction,
    String otpRequestId,
    String maskedMobile,
    String globalUid,
    String customerId,
    String failureReason,
    List<StepInfo> steps,
    String timestamp
) {}
