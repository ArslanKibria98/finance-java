package com.ksa.financing.collections.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateRepaymentScheduleRequest(
        @NotNull UUID loanId,
        UUID productId,   // optional — enables per-product DelinquencyRule lookups at payment time
        @NotNull String scheduleNumber,
        @NotNull @Positive BigDecimal totalPrincipal,
        @NotNull BigDecimal totalProfit,
        @NotNull LocalDate firstDueDate,
        @NotNull LocalDate lastDueDate,
        @NotNull @NotEmpty @Valid List<InstallmentEntryRequest> installments
) {
    public record InstallmentEntryRequest(
            @Positive int installmentNumber,
            @NotNull LocalDate dueDate,
            @NotNull @Positive BigDecimal principalAmount,
            @NotNull BigDecimal profitAmount,
            BigDecimal feeAmount
    ) {}
}
