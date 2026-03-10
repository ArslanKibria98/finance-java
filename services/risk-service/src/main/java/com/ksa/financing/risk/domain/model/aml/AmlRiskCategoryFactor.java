package com.ksa.financing.risk.domain.model.aml;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A factor option within a risk category.
 * Each factor has a weight percentage and pre-computed rating.
 * rating = category.weight * (factorWeightPct / 100)
 */
public record AmlRiskCategoryFactor(
    UUID id,
    UUID categoryId,
    String factorCode,
    String nameEn,
    String nameAr,
    BigDecimal factorWeightPct,
    BigDecimal computedRating,
    int sortOrder,
    boolean active
) {}
