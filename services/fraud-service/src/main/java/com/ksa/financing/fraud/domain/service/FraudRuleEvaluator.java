package com.ksa.financing.fraud.domain.service;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;

/**
 * Interface for individual fraud rule evaluators.
 * Each of the 30 HLD rules implements this interface.
 * Implementations live in infrastructure/rule/ (NOT in domain).
 */
public interface FraudRuleEvaluator {

    /**
     * The rule ID this evaluator handles (e.g., LOC_001, DEV_001, TMO_001).
     */
    FraudRuleId getRuleId();

    /**
     * Evaluates the fraud event against this rule's logic using the configurable parameters from the FraudRule.
     *
     * @param event the enriched fraud event to evaluate
     * @param rule  the tenant-specific rule configuration (thresholds, action, status)
     * @return the evaluation result (triggered or not, with decision and score contribution)
     */
    RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule);
}
