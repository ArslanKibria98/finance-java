package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateTransferRequest(
        @NotNull UUID sourceWalletId,
        UUID destinationWalletId,
        String destinationWalletNumber,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be > 0") BigDecimal amount,
        String currency,
        String purposeNote,
        String channel
) {}
