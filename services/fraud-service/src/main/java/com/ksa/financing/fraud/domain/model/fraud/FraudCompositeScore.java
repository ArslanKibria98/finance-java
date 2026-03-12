package com.ksa.financing.fraud.domain.model.fraud;

import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;

import java.util.List;

public record FraudCompositeScore(
    int score,
    String riskLevel,
    List<RuleEvaluationResult> triggeredRules
) {
    public static String levelFromScore(int score) {
        if (score >= 81) return "CRITICAL";
        if (score >= 61) return "HIGH";
        if (score >= 31) return "MEDIUM";
        return "LOW";
    }
}
