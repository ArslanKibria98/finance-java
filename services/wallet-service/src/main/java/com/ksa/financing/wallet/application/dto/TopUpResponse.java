package com.ksa.financing.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TopUpResponse(
    UUID id,
    String transactionNumber,
    String method,
    BigDecimal amount,
    BigDecimal feeAmount,
    BigDecimal netAmount,
    String status,
    Instant initiatedAt,
    Instant completedAt
) {}
