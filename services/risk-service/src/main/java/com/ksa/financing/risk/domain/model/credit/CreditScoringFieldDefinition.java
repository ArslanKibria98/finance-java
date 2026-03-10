package com.ksa.financing.risk.domain.model.credit;

import java.util.UUID;

public record CreditScoringFieldDefinition(
        UUID id,
        UUID tenantId,
        String fieldKey,
        String nameEn,
        String nameAr,
        String dataType,
        boolean active,
        int sortOrder
) {}
