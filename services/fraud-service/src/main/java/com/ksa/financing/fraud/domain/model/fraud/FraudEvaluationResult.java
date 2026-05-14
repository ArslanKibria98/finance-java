package com.ksa.financing.fraud.domain.model.fraud;

import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FraudEvaluationResult(
    UUID id,
    UUID tenantId,
    String eventId,
    UUID fraudEventId,
    String customerId,
    FraudDecision decision,
    FraudBlockType blockType,
    UUID blockCodeId,
    String blockCode,
    int compositeRiskScore,
    String riskLevel,
    List<RuleEvaluationResult> triggeredRules,
    String blockReason,
    Integer blockDurationHours,
    String customerMessage,
    long evaluationTimeMs,
    LocalDateTime evaluatedAt
) {}
