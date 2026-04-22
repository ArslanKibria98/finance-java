package com.ksa.financing.collections.adapter.rest.request;

import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.SettlementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InitiateSettlementRequest(
        @NotNull UUID loanId,
        @NotNull SettlementType settlementType,
        @NotNull @Positive BigDecimal settlementAmount,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank String idempotencyKey,
        LocalDate settlementDate,
        String notes
) {}
