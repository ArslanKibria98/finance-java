package com.ksa.financing.onboarding.foreign.application.dto;

import java.util.Map;

public record ForeignStatusResponse(
        String workflowId,
        String status,
        String currentStep,
        String nextAction,
        String email,
        String mobileNumber,
        String countryOfOrigin,
        String residentialCountry,
        String customerId,
        String walletId,
        String keycloakUserId,
        String globalUid,
        Double faceMatchScore,
        boolean pinSet,
        boolean biometricsEnabled,
        boolean onboardingComplete,
        Map<String, Object> extractedData,
        Map<String, Object> confirmedData,
        String failureReason,
        String startedAt,
        String lastUpdatedAt
) {
}
