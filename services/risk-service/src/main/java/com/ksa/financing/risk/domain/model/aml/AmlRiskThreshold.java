package com.ksa.financing.risk.domain.model.aml;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Configurable risk level threshold.
 * Defines score ranges for HIGH, MEDIUM, LOW classification.
 */
public record AmlRiskThreshold(
    UUID id,
    AmlRiskLevel riskLevel,
    BigDecimal minScore,
    BigDecimal maxScore,
    String description
) {}
