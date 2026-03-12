package com.ksa.financing.fraud.domain.service;

import com.ksa.financing.fraud.domain.model.fraud.FraudBlockType;
import com.ksa.financing.fraud.domain.model.fraud.FraudCompositeScore;
import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;

/**
 * Consolidates rule evaluation results into a final decision.
 * Decision priority: BLOCK > HOLD > ALERT > ALLOW
 * Pure domain service: no framework imports.
 */
public class FraudDecisionEngine {

    /**
     * Determines the final fraud decision from the composite score.
     * Takes the highest-severity decision from all triggered rules.
     * If no rules triggered, returns ALLOW.
     */
    public FraudDecision determineDecision(FraudCompositeScore compositeScore) {
        if (compositeScore.triggeredRules().isEmpty()) {
            return FraudDecision.ALLOW;
        }

        var finalDecision = FraudDecision.ALLOW;
        for (var result : compositeScore.triggeredRules()) {
            if (result.decision() != null) {
                finalDecision = FraudDecision.higher(finalDecision, result.decision());
            }
        }
        return finalDecision;
    }

    /**
     * Determines the block type from the highest-priority triggered rule that has a block type.
     * Returns null if no rule specifies a block type.
     */
    public FraudBlockType determineBlockType(FraudCompositeScore compositeScore) {
        return compositeScore.triggeredRules().stream()
                .filter(r -> r.blockType() != null)
                .map(RuleEvaluationResult::blockType)
                .reduce((a, b) -> {
                    // PERMANENT > TEMPORARY > SESSION > APPLICATION > DISBURSEMENT > PAYMENT
                    if (a == FraudBlockType.PERMANENT || b == FraudBlockType.PERMANENT) {
                        return FraudBlockType.PERMANENT;
                    }
                    return a.ordinal() <= b.ordinal() ? a : b;
                })
                .orElse(null);
    }

    /**
     * Builds a human-readable block reason from triggered rules.
     */
    public String buildBlockReason(FraudCompositeScore compositeScore) {
        if (compositeScore.triggeredRules().isEmpty()) {
            return null;
        }

        var reasons = compositeScore.triggeredRules().stream()
                .filter(r -> r.detail() != null)
                .map(r -> r.ruleId().name() + ": " + r.detail())
                .toList();

        return String.join("; ", reasons);
    }
}
