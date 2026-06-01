package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payload to create or update ONE general scoring criterion + its rules.
 * Used by tenant-level admin CRUD (POST / PUT /general/criteria[/{id}]).
 */
public record SaveSingleGeneralCriteriaRequest(
        UUID fieldDefinitionId,
        String customName,
        boolean custom,
        boolean enabled,
        int sortOrder,
        @Valid List<RuleItem> rules
) {
    public record RuleItem(
            String operator,
            String value,
            BigDecimal weight,
            BigDecimal percentage
    ) {}
}
