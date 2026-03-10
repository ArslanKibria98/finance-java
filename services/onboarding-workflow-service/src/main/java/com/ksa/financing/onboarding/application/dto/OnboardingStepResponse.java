package com.ksa.financing.onboarding.application.dto;

import java.util.List;
import java.util.Map;

/**
 * Common response for onboarding step transitions (accept-terms, nafath-callback, submit-info, resend-otp).
 */
public record OnboardingStepResponse(
    String workflowId,
    String status,
    String currentStep,
    String nextAction,
    String globalUid,
    String customerId,
    String message,
    String failureReason,
    Map<String, Object> nafathVerificationData,
    List<StepInfo> steps,
    String timestamp
) {}
