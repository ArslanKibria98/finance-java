package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import org.springframework.stereotype.Component;

/**
 * TMO_006 — Disbursement Amount Mismatch (PERMANENT escalation of FIN_002).
 */
@Component
public class DisbursementAmountMismatchEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_006;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.DISBURSEMENT
                || event.transactionAmount() == null
                || event.approvedLoanAmount() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (event.transactionAmount().compareTo(event.approvedLoanAmount()) == 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Disbursed " + event.transactionAmount() + " vs approved " + event.approvedLoanAmount(),
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
