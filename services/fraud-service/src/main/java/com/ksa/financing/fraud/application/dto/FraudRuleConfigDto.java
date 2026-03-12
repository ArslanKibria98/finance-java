package com.ksa.financing.fraud.application.dto;

import java.util.List;
import java.util.UUID;

public record FraudRuleConfigDto(
    UUID id,
    String ruleId,
    String scenarioName,
    String scenarioNameAr,
    String category,
    String detectionLogic,
    String defaultAction,
    String blockType,
    String status,
    List<RuleParameterDto> parameters,
    int priority
) {
    public record RuleParameterDto(
        String key,
        String value,
        String dataType,
        String description,
        String descriptionAr
    ) {}
}
