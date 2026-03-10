package com.ksa.financing.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletMovementResponse(
    UUID id,
    String movementNumber,
    String movementType,
    String purpose,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    String description,
    Instant createdAt
) {}
