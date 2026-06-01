package com.ksa.financing.onboarding.canada.application.dto;

import com.ksa.financing.onboarding.canada.domain.model.DocumentType;

import java.util.Map;

public record CanadaStatusResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String email,
        String mobileNumber,
        DocumentType documentType,
        String customerId,
        String walletId,
        String keycloakUserId,
        String globalUid,
        Double faceMatchScore,
        boolean pinSet,
        boolean biometricsEnabled,
        Map<String, Object> extractedData,
        Map<String, Object> confirmedData,
        String failureReason,
        String startedAt,
        String lastUpdatedAt
) {
}
