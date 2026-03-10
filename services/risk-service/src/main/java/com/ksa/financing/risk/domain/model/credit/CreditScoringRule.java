package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditScoringRule(
        UUID id,
        UUID tenantId,
        UUID criteriaId,
        CreditScoringOperator operator,
        String value,
        BigDecimal weight,
        BigDecimal percentage
) {}
