package com.ksa.financing.fraud.domain.model.rule;

import com.ksa.financing.fraud.domain.model.fraud.FraudBlockType;
import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;
import java.util.UUID;

public record RuleEvaluationResult(
    FraudRuleId ruleId,
    boolean triggered,
    FraudDecision decision,
    FraudBlockType blockType,
    UUID blockCodeId,
    String blockCode,
    String detail,
    int scoreContribution
) {
    public static RuleEvaluationResult notTriggered(FraudRuleId ruleId) {
        return new RuleEvaluationResult(ruleId, false, null, null, null, null, null, 0);
    }

    public static RuleEvaluationResult triggered(FraudRuleId ruleId, FraudDecision decision,
                                                  FraudBlockType blockType, UUID blockCodeId, String blockCode,
                                                  String detail, int scoreContribution) {
        return new RuleEvaluationResult(ruleId, true, decision, blockType, blockCodeId, blockCode, detail, scoreContribution);
    }
}
