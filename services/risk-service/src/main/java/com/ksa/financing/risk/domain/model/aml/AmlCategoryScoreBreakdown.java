package com.ksa.financing.risk.domain.model.aml;

import java.math.BigDecimal;

/**
 * Score contribution from a single category in the AML assessment.
 */
public record AmlCategoryScoreBreakdown(
    String categoryCode,
    String categoryName,
    String matchedFactor,
    BigDecimal categoryWeight,
    BigDecimal factorWeightPct,
    BigDecimal rating
) {}
