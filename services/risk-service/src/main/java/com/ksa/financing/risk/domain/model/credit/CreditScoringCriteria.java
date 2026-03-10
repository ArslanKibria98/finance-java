package com.ksa.financing.risk.domain.model.credit;

import java.util.List;
import java.util.UUID;

public record CreditScoringCriteria(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID fieldDefinitionId,
        String customName,
        boolean custom,
        boolean enabled,
        int sortOrder,
        List<CreditScoringRule> rules
) {}
