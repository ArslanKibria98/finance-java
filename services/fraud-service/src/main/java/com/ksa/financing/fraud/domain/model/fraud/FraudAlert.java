package com.ksa.financing.fraud.domain.model.fraud;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudAlert(
    UUID id,
    UUID tenantId,
    UUID evaluationId,
    String customerId,
    String triggeringRuleId,
    FraudAlertPriority priority,
    FraudAlertStatus status,
    FraudDecision decision,
    String summary,
    String summaryAr,
    String details,
    UUID assignedTo,
    LocalDateTime assignedAt,
    UUID resolvedBy,
    LocalDateTime resolvedAt,
    String resolutionNote,
    LocalDateTime slaDeadline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int version
) {}
