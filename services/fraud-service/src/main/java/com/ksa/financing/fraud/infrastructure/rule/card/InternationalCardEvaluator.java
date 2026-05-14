package com.ksa.financing.fraud.infrastructure.rule.card;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import org.springframework.stereotype.Component;

/**
 * CARD_001 — International Card Used for Installment.
 * Trigger: paymentSource.cardCountry != local country (default SA).
 */
@Component
public class InternationalCardEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.CARD_001;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.REPAYMENT
                || event.paymentSource() == null
                || event.paymentSource().cardCountry() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        String localCountry = rule.getParameterValue("local_country", "SA");
        if (localCountry.equalsIgnoreCase(event.paymentSource().cardCountry())) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "International card from " + event.paymentSource().cardCountry(),
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
