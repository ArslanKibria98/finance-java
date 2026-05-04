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
 * TMO_002 — Application Rate Limit Breach (Monthly / Annual).
 * Trigger: > 2 per month OR > 4 per year (BLOCK action).
 */
@Component
@RequiredArgsConstructor
public class AppRateLimitEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_002;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int maxMonth = rule.getParameterInt("max_per_month", 2);
        int maxYear = rule.getParameterInt("max_per_year", 4);
        var now = event.eventTimestamp();

        long monthCount = fraudEventRepository.countByCustomerAndType(
                event.tenantId(), event.customerId(),
                FraudEventType.LOAN_APPLICATION.name(), now.minusDays(30));
        long yearCount = fraudEventRepository.countByCustomerAndType(
                event.tenantId(), event.customerId(),
                FraudEventType.LOAN_APPLICATION.name(), now.minusDays(365));

        if (monthCount <= maxMonth && yearCount <= maxYear) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Rate-limit breach month=" + monthCount + " year=" + yearCount,
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
