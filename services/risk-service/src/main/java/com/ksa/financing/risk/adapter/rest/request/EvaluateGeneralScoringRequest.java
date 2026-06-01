package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record EvaluateGeneralScoringRequest(
        UUID customerId,
        String workflowId,
        String stage,
        @NotNull Map<String, String> answers,
        Boolean persistSnapshot
) {
    public boolean shouldPersist() {
        return Boolean.TRUE.equals(persistSnapshot);
    }
}
