package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import org.springframework.stereotype.Component;

/**
 * TMO_013 — Unusual Transaction Time-of-Day.
 * Trigger: event timestamp hour ∈ [01:00, 04:00].
 */
@Component
public class UnusualTimeOfDayEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_013;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventTimestamp() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int start = rule.getParameterInt("unusual_hour_start", 1);
        int end = rule.getParameterInt("unusual_hour_end", 4);
        int hour = event.eventTimestamp().getHour();
        if (hour < start || hour >= end) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Activity at unusual hour: " + hour + ":00",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
