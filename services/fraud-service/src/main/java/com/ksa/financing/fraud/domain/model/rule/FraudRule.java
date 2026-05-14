package com.ksa.financing.fraud.domain.model.rule;

import com.ksa.financing.fraud.domain.model.fraud.FraudBlockType;
import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FraudRule(
    UUID id,
    UUID tenantId,
    FraudRuleId ruleId,
    String scenarioName,
    String scenarioNameAr,
    FraudRuleCategory category,
    String detectionLogic,
    FraudDecision defaultAction,
    FraudBlockType blockType,
    UUID blockCodeId,
    String blockCode,
    FraudRuleStatus status,
    List<RuleParameter> parameters,
    int priority,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Gets a parameter value by key, returns defaultValue if not found.
     */
    public String getParameterValue(String key, String defaultValue) {
        if (parameters == null) return defaultValue;
        return parameters.stream()
                .filter(p -> key.equals(p.key()))
                .map(RuleParameter::value)
                .findFirst()
                .orElse(defaultValue);
    }

    public int getParameterInt(String key, int defaultValue) {
        var val = getParameterValue(key, null);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public boolean getParameterBoolean(String key, boolean defaultValue) {
        var val = getParameterValue(key, null);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }
}
