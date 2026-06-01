package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payload to replace ALL general (onboarding) scoring criteria for the
 * caller's tenant. Mirrors {@link SaveCreditScoringRequest} but without
 * the productId path variable.
 */
public record SaveGeneralScoringRequest(
        @Valid @NotNull List<GeneralScoringCriteriaItem> criteria
) {
    public record GeneralScoringCriteriaItem(
            UUID fieldDefinitionId,
            String customName,
            boolean custom,
            boolean enabled,
            int sortOrder,
            @Valid List<GeneralScoringRuleItem> rules
    ) {}

    public record GeneralScoringRuleItem(
            @NotNull String operator,
            @NotNull String value,
            @NotNull BigDecimal weight,
            @NotNull BigDecimal percentage
    ) {}
}
