package com.ksa.financing.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
    UUID id,
    String walletNumber,
    String accountNumber,
    UUID customerId,
    BigDecimal availableBalance,
    BigDecimal reservedBalance,
    BigDecimal totalBalance,
    String currency,
    String status,
    boolean autoDebitEnabled,
    Long fineractSavingsAccountId,
    boolean ledgerSynced,
    Instant createdAt,
    Instant updatedAt
) {}
