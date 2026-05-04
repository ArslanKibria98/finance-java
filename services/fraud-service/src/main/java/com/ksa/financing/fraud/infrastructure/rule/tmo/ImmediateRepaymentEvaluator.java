package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.LendingPort;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * TMO_008 — Same-Day or Near-Immediate Full Repayment (AML signal).
 * Trigger: full repayment within 48h of disbursement.
 */
@Component
@RequiredArgsConstructor
public class ImmediateRepaymentEvaluator implements FraudRuleEvaluator {

    private final LendingPort lendingPort;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_008;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.EARLY_REPAYMENT
                || event.loanApplicationId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int windowHours = rule.getParameterInt("repayment_window_hours", 48);
        var disb = lendingPort.fetchDisbursement(event.tenantId(), event.loanApplicationId());
        if (disb.isEmpty() || disb.get().disbursedAt() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        long hoursSince = Duration.between(disb.get().disbursedAt(), event.eventTimestamp()).toHours();
        if (hoursSince > windowHours) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Full repayment " + hoursSince + "h after disbursement",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
