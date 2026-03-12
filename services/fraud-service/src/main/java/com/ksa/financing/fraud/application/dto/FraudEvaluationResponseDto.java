package com.ksa.financing.fraud.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FraudEvaluationResponseDto(
    UUID evaluationId,
    String eventId,
    String customerId,
    String decision,
    String blockType,
    int compositeRiskScore,
    String riskLevel,
    List<TriggeredRuleDto> triggeredRules,
    String blockReason,
    Integer blockDurationHours,
    String customerMessage,
    long evaluationTimeMs,
    LocalDateTime evaluatedAt
) {
    public record TriggeredRuleDto(
        String ruleId,
        String decision,
        String blockType,
        String detail,
        int scoreContribution
    ) {}
}
