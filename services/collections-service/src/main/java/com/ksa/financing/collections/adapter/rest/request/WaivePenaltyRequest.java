package com.ksa.financing.collections.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record WaivePenaltyRequest(
        @NotNull UUID loanId,
        @NotNull UUID installmentId,
        @Positive BigDecimal amount,   // null = waive full remaining penalty
        @NotBlank String reason,
        String approvalReference
) {}
