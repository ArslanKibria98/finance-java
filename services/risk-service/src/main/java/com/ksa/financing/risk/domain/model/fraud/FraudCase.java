package com.ksa.financing.risk.domain.model.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FraudCase(
    UUID id,
    UUID tenantId,
    String caseNumber,
    String customerId,
    FraudCaseStatus status,
    UUID assignedTo,
    String summary,
    BigDecimal estimatedLoss,
    LocalDateTime createdAt,
    LocalDateTime closedAt,
    String closureNote,
    LocalDateTime updatedAt,
    int version
) {}
