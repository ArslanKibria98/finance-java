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
 * TMO_004 — Late IBAN Substitution Before Disbursement.
 * Trigger: IBAN updated within 24 hours of scheduled disbursement.
 */
@Component
@RequiredArgsConstructor
public class LateIbanSubstitutionEvaluator implements FraudRuleEvaluator {

    private final LendingPort lendingPort;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_004;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.IBAN_UPDATE || event.loanApplicationId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int minHours = rule.getParameterInt("min_hours_before_disbursement", 24);
        var disb = lendingPort.fetchDisbursement(event.tenantId(), event.loanApplicationId());
        if (disb.isEmpty() || disb.get().scheduledAt() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var hoursToDisbursement = Duration.between(event.eventTimestamp(), disb.get().scheduledAt()).toHours();
        if (hoursToDisbursement < 0 || hoursToDisbursement >= minHours) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "IBAN updated " + hoursToDisbursement + "h before disbursement",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
