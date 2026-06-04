package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to send money from a user wallet to an external Canadian bank account via Scotia RTP.
 * If {@code sourceWalletId} is omitted, the wallet is resolved from the authenticated user.
 */
public record InitiateExternalTransferRequest(
        UUID sourceWalletId,
        String counterpartyName,
        @NotBlank String counterpartyAccount,
        String counterpartyEmail,
        String counterpartyBankCode,
        @NotNull @Positive BigDecimal amount,
        String currency,
        String purposeNote
) {}
