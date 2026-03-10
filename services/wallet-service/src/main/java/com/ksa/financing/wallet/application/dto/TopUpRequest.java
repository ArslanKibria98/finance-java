package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TopUpRequest(
    @NotNull @Positive BigDecimal amount,
    @NotBlank String method,
    String sourceIban,
    @NotBlank String idempotencyKey
) {}
