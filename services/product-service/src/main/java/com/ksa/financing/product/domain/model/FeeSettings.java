package com.ksa.financing.product.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeSettings(
    UUID id,
    BigDecimal minFinancingAmount,
    BigDecimal maxFinancingAmount,
    BigDecimal vatPercentage,
    BigDecimal revenueEligibilityThreshold,
    BigDecimal maxDbrPercentage,
    BigDecimal globalDbrPercentage,
    String dbrCalculationMethod,
    String dbrExceptions
) {}
