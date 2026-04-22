package com.ksa.financing.collections.adapter.rest.request;

import com.ksa.financing.collections.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProcessPaymentRequest(
        @NotNull UUID loanId,
        UUID installmentId,
        @NotEmpty List<@jakarta.validation.constraints.NotBlank String> invoiceId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        String idempotencyKey,
        String providerReference,
        String returnUrl,
        String notes
) {}
