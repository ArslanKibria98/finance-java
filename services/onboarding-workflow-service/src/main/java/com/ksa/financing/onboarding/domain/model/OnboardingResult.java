package com.ksa.financing.onboarding.domain.model;

import java.util.UUID;

public record OnboardingResult(
    String workflowId,
    OnboardingStatus status,
    UUID customerId,
    UUID globalUid,
    UUID walletId,
    String keycloakUserId,
    String failureReason
) {}
