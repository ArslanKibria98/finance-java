package com.ksa.financing.fraud.infrastructure.rule.financial;

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
 * FIN_003 — Excessive Loan Applications.
 * Trigger: > 2 per month OR > 4 per year.
 */
@Component
@RequiredArgsConstructor
public class ExcessiveLoanApplicationsEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.FIN_003;
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
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Apps month=" + monthCount + "/" + maxMonth + " year=" + yearCount + "/" + maxYear,
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
