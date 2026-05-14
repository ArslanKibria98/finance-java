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
 * TMO_010 — Repeated Payment Reversal / Refund Pattern.
 * Trigger: 2+ reversals per loan OR 3+ across all loans.
 */
@Component
@RequiredArgsConstructor
public class RepeatedReversalEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_010;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.REPAYMENT || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int maxPerLoan = rule.getParameterInt("max_reversals_per_loan", 2);
        int maxTotal = rule.getParameterInt("max_reversals_total", 3);

        long perLoan = event.loanApplicationId() != null
                ? fraudEventRepository.countReversalsForLoan(event.tenantId(), event.customerId(), event.loanApplicationId())
                : 0;
        long total = fraudEventRepository.countReversalsForCustomer(event.tenantId(), event.customerId());

        if (perLoan < maxPerLoan && total < maxTotal) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Reversals: per-loan=" + perLoan + " total=" + total,
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
