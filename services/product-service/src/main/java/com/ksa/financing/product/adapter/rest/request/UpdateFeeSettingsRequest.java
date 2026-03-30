package com.ksa.financing.product.adapter.rest.request;

import java.math.BigDecimal;

public record UpdateFeeSettingsRequest(
    BigDecimal minFinancingAmount,
    BigDecimal maxFinancingAmount,
    BigDecimal vatPercentage,
    BigDecimal revenueEligibilityThreshold,
    BigDecimal maxDbrPercentage,
    BigDecimal globalDbrPercentage,
    String dbrCalculationMethod,
    String dbrExceptions
) {}
