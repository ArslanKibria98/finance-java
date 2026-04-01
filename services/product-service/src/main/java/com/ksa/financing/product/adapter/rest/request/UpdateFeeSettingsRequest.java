package com.ksa.financing.product.adapter.rest.request;

import java.math.BigDecimal;

public record UpdateFeeSettingsRequest(
    BigDecimal revenueEligibilityThreshold,
    BigDecimal maxDbrPercentage,
    BigDecimal globalDbrPercentage,
    String dbrCalculationMethod,
    String dbrExceptions
) {}
