package com.ksa.financing.product.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeSettings(
    UUID id,
    BigDecimal revenueEligibilityThreshold,
    BigDecimal maxDbrPercentage,
    String dbrCalculationMethod,
    String dbrExceptions,
    BigDecimal maxDti,
    Integer minAge,
    Integer maxAge,
    BigDecimal gdbrPercentage
) {}
