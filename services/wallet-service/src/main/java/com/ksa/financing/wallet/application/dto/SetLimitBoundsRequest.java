package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin request to set the platform transaction-limit bounds for the tenant. */
public record SetLimitBoundsRequest(
        @NotNull @DecimalMin("0") BigDecimal minDailyLimit,
        @NotNull @DecimalMin("0") BigDecimal maxDailyLimit,
        @NotNull @DecimalMin("0") BigDecimal minMonthlyLimit,
        @NotNull @DecimalMin("0") BigDecimal maxMonthlyLimit,
        @NotNull @DecimalMin("0") BigDecimal minYearlyLimit,
        @NotNull @DecimalMin("0") BigDecimal maxYearlyLimit,
        @NotNull @DecimalMin("0") BigDecimal defaultDailyLimit,
        @NotNull @DecimalMin("0") BigDecimal defaultMonthlyLimit,
        @NotNull @DecimalMin("0") BigDecimal defaultYearlyLimit) {
}
