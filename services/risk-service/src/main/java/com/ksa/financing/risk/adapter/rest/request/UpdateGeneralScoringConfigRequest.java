package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateGeneralScoringConfigRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal minPassPercentage,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal greenThreshold,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal amberThreshold,
        boolean enabled
) {}
