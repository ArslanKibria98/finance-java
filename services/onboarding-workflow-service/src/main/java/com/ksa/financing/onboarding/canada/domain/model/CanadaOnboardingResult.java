package com.ksa.financing.onboarding.canada.domain.model;

import java.io.Serializable;

public record CanadaOnboardingResult(
        String workflowId,
        String customerId,
        String walletId,
        String keycloakUserId,
        CanadaOnboardingStep finalStep,
        String failureReason
) implements Serializable {
}
