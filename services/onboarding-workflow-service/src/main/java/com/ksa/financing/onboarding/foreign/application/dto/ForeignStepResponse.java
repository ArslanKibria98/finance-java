package com.ksa.financing.onboarding.foreign.application.dto;

public record ForeignStepResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        boolean onboardingComplete,
        String message,
        String failureReason,
        String timestamp,
        // Echoed for log/audit traceability so Kibana can filter the full
        // onboarding journey by phone number, not just the initiate step.
        String mobileNumber
) {
}
