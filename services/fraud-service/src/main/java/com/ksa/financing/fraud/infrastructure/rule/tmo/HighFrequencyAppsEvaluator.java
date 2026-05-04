package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudEventRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TMO_001 — High-Frequency Loan Applications (Velocity).
 * Trigger: > 3 applications by same customer within 24h rolling window.
 */
@Component
@RequiredArgsConstructor
public class HighFrequencyAppsEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_001;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int max = rule.getParameterInt("max_applications", 3);
        int windowHours = rule.getParameterInt("rolling_window_hours", 24);

        long count = fraudEventRepository.countByCustomerAndType(
                event.tenantId(), event.customerId(),
                FraudEventType.LOAN_APPLICATION.name(),
                event.eventTimestamp().minusHours(windowHours));
        if (count <= max) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                count + " apps in " + windowHours + "h (limit " + max + ")",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
