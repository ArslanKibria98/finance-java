package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferByMobileRequest(
        @NotBlank String receiverMobile,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be > 0") BigDecimal amount,
        String currency,
        String purposeNote
) {}
