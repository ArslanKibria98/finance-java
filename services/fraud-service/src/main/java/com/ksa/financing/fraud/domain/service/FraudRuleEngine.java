package com.ksa.financing.fraud.domain.service;

import com.ksa.financing.fraud.domain.model.fraud.FraudCompositeScore;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleStatus;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;

/**
 * Core fraud rule engine — evaluates all active rules against an enriched fraud event.
 * Pure domain service: no framework imports.
 */
public class FraudRuleEngine {

    private final Map<String, FraudRuleEvaluator> evaluatorsByRuleId;

    public FraudRuleEngine(Map<String, FraudRuleEvaluator> evaluatorsByRuleId) {
        this.evaluatorsByRuleId = evaluatorsByRuleId;
    }

    /**
     * Evaluates all active rules against the fraud event.
     * Rules are evaluated in priority order (lower number = higher priority).
     * Score is capped at 100.
     *
     * @param event       the enriched fraud event
     * @param activeRules the tenant's active rule configurations, sorted by priority
     * @return composite score with all triggered rules
     */
    public FraudCompositeScore evaluate(FraudEvent event, List<FraudRule> activeRules) {
        var sortedRules = activeRules.stream()
                .filter(r -> r.status() == FraudRuleStatus.ACTIVE)
                .sorted(Comparator.comparingInt(FraudRule::priority))
                .toList();

        var triggeredResults = new ArrayList<RuleEvaluationResult>();
        int totalScore = 0;

        for (var rule : sortedRules) {
            var evaluator = evaluatorsByRuleId.get(rule.ruleId().name());
            if (evaluator == null) {
                continue;
            }

            var result = evaluator.evaluate(event, rule);
            if (result.triggered()) {
                triggeredResults.add(result);
                totalScore += result.scoreContribution();
            }
        }

        int cappedScore = Math.min(totalScore, 100);
        var riskLevel = FraudCompositeScore.levelFromScore(cappedScore);

        // Resolve overall block code (from highest priority rule that resulted in BLOCK or HOLD)
        UUID overallBlockCodeId = null;
        String overallBlockCode = null;

        for (var result : triggeredResults) {
            if (result.decision() == FraudDecision.BLOCK || result.decision() == FraudDecision.HOLD) {
                if (result.blockCodeId() != null) {
                    overallBlockCodeId = result.blockCodeId();
                    overallBlockCode = result.blockCode();
                    break; // Since rules are sorted by priority, first one wins
                }
            }
        }

        return new FraudCompositeScore(cappedScore, riskLevel, triggeredResults, overallBlockCodeId, overallBlockCode);
    }
}
