package com.ksa.financing.onboarding.foreign.domain.model;

import java.io.Serializable;

public record ForeignOnboardingResult(
        String workflowId,
        String customerId,
        String walletId,
        String keycloakUserId,
        ForeignOnboardingStep finalStep,
        String failureReason
) implements Serializable {
}
