package com.ksa.financing.risk.domain.model.aml;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A risk scoring category with weight and type.
 * Pure domain model — no framework annotations.
 */
public record AmlRiskCategory(
    UUID id,
    String categoryCode,
    String nameEn,
    String nameAr,
    AmlCategoryType categoryType,
    BigDecimal weight,
    int sortOrder,
    boolean active
) {}
