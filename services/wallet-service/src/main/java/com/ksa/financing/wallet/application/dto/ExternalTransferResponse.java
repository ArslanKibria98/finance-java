package com.ksa.financing.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExternalTransferResponse(
        UUID id,
        String transferNumber,
        String direction,
        UUID walletId,
        String accountNumber,
        String counterpartyName,
        String counterpartyAccount,
        BigDecimal amount,
        String currency,
        String status,
        String purposeNote,
        UUID movementId,
        UUID ledgerEntryId,
        String scotiaPaymentId,
        String scotiaClearingRef,
        String scotiaStatus,
        boolean counterpartyInternal,
        UUID counterpartyWalletId,
        UUID counterpartyMovementId,
        String errorCode,
        String errorMessage,
        Instant initiatedAt,
        Instant completedAt
) {}
