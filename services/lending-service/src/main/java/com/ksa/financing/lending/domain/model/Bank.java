package com.ksa.financing.lending.domain.model;

import java.util.UUID;

/**
 * Domain model for bank reference data.
 */
public record Bank(
        UUID id,
        UUID tenantId,
        String code,
        String nameEn,
        String nameAr,
        boolean active,
        int sortOrder
) {
}
