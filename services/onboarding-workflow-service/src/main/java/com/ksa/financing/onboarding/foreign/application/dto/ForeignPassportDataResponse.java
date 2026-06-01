package com.ksa.financing.onboarding.foreign.application.dto;

import java.util.Map;

public record ForeignPassportDataResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String faciaReferenceId,
        Map<String, Object> extractedData,
        String message,
        String failureReason,
        String timestamp,
        // Echoed for log/audit traceability so Kibana can filter the full
        // onboarding journey by phone number, not just the initiate step.
        String mobileNumber
) {
}
