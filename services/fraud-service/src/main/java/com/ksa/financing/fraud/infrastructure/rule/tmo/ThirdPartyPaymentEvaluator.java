package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.CustomerProfilePort;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TMO_009 — Installment Payment from Third-Party Source.
 * Trigger: payment IBAN/card not registered to borrower.
 */
@Component
@RequiredArgsConstructor
public class ThirdPartyPaymentEvaluator implements FraudRuleEvaluator {

    private final CustomerProfilePort customerProfilePort;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_009;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.REPAYMENT
                || event.paymentSource() == null
                || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (event.paymentSource().thirdParty()) {
            return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                    "Payment flagged as third-party",
                    EvaluatorScores.forDecision(rule.defaultAction()));
        }
        var profile = customerProfilePort.fetchProfile(event.tenantId(), event.customerId());
        if (profile.isEmpty()) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var registeredIban = profile.get().registeredIban();
        var paymentIban = event.paymentSource().iban();
        if (registeredIban != null && paymentIban != null && !registeredIban.equalsIgnoreCase(paymentIban)) {
            return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                    "Payment IBAN does not match registered IBAN",
                    EvaluatorScores.forDecision(rule.defaultAction()));
        }
        return RuleEvaluationResult.notTriggered(rule.ruleId());
    }
}
