package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tenant-level configuration for general (onboarding) credit scoring thresholds.
 * Defines the Green / Amber / Red bands used by the decision engine when scoring
 * customers during onboarding (product-agnostic).
 */
public record GeneralScoringConfig(
        UUID id,
        UUID tenantId,
        BigDecimal minPassPercentage,
        BigDecimal greenThreshold,
        BigDecimal amberThreshold,
        boolean enabled
) {
    public static GeneralScoringConfig defaults(UUID tenantId) {
        return new GeneralScoringConfig(
                null,
                tenantId,
                new BigDecimal("50.00"),
                new BigDecimal("75.00"),
                new BigDecimal("50.00"),
                true);
    }
}
