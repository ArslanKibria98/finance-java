package com.ksa.financing.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        String transferNumber,
        UUID sourceWalletId,
        UUID destinationWalletId,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDebit,
        String currency,
        String status,
        String channel,
        String purposeNote,
        UUID debitMovementId,
        UUID creditMovementId,
        String errorCode,
        String errorMessage,
        Instant initiatedAt,
        Instant completedAt
) {}
