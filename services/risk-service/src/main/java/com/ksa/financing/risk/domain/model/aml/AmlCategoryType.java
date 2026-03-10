package com.ksa.financing.risk.domain.model.aml;

/**
 * Category type for AML scoring.
 * DOMINANT: Auto-override to HIGH risk if matched (e.g., PEP, Internal List).
 * MUTUAL_EXCLUSIVE: Contributes weighted score to the total.
 */
public enum AmlCategoryType {
    DOMINANT,
    MUTUAL_EXCLUSIVE
}
