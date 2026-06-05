package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Customer request to raise (or change) their wallet transaction limits. */
public record RequestLimitChangeRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal requestedSingleLimit,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal requestedDailyLimit,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal requestedWeeklyLimit,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal requestedMonthlyLimit,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal requestedYearlyLimit,
        @Size(max = 500) String reason) {
}
