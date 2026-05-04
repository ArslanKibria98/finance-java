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
 * TMO_005 — Multiple Disbursements to Same Beneficiary IBAN (cross-account).
 * Trigger: 2+ customer accounts share same disbursement_iban.
 */
@Component
@RequiredArgsConstructor
public class MultipleDisbursementsSameIbanEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_005;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.DISBURSEMENT || event.disbursementIban() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int min = rule.getParameterInt("min_distinct_accounts", 2);
        long count = fraudEventRepository.countDistinctCustomersByIban(
                event.disbursementIban(),
                event.eventTimestamp().minusYears(1));
        if (count < min) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                count + " accounts share IBAN " + event.disbursementIban(),
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
