package com.ksa.financing.product.adapter.rest.request;

import java.math.BigDecimal;

public record UpdateFeeSettingsRequest(
    BigDecimal revenueEligibilityThreshold,
    BigDecimal maxDbrPercentage,
    String dbrCalculationMethod,
    String dbrExceptions,
    BigDecimal maxDti,
    Integer minAge,
    Integer maxAge,
    BigDecimal gdbrPercentage
) {}
