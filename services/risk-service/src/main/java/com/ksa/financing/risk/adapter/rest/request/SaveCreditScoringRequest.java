package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaveCreditScoringRequest(
        @Valid @NotNull List<CreditScoringCriteriaItem> criteria
) {
    public record CreditScoringCriteriaItem(
            UUID fieldDefinitionId,
            String customName,
            boolean custom,
            boolean enabled,
            int sortOrder,
            @Valid List<CreditScoringRuleItem> rules
    ) {}

    public record CreditScoringRuleItem(
            @NotNull String operator,
            @NotNull String value,
            @NotNull BigDecimal weight,
            @NotNull BigDecimal percentage
    ) {}
}
