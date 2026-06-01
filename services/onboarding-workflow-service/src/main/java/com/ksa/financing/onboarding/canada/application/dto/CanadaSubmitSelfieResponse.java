package com.ksa.financing.onboarding.canada.application.dto;

public record CanadaSubmitSelfieResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String faciaReferenceId,
        Double faceMatchScore,
        String customerId,
        String walletId,
        String message,
        String failureReason,
        String timestamp,
        // Echoed for log/audit traceability so Kibana can filter the full
        // onboarding journey by phone number, not just the initiate step.
        String mobileNumber
) {
}
