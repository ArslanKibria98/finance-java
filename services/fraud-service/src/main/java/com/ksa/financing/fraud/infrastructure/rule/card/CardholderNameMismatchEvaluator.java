package com.ksa.financing.fraud.infrastructure.rule.card;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.CustomerProfilePort;
import com.ksa.financing.fraud.domain.port.out.NameSimilarityPort;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CARD_003 — Cardholder Name Mismatch.
 * Trigger: paymentSource.cardHolderName fuzzy-mismatched with customer.fullName.
 */
@Component
@RequiredArgsConstructor
public class CardholderNameMismatchEvaluator implements FraudRuleEvaluator {

    private final CustomerProfilePort customerProfilePort;
    private final NameSimilarityPort nameSimilarityPort;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.CARD_003;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.REPAYMENT
                || event.paymentSource() == null
                || event.paymentSource().cardHolderName() == null
                || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        boolean useFuzzy = rule.getParameterBoolean("use_fuzzy_match", true);
        var profile = customerProfilePort.fetchProfile(event.tenantId(), event.customerId());
        if (profile.isEmpty() || profile.get().fullName() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        String customerName = profile.get().fullName();
        String cardName = event.paymentSource().cardHolderName();
        boolean matches = useFuzzy
                ? nameSimilarityPort.matches(customerName, cardName, 0.85)
                : customerName.equalsIgnoreCase(cardName);
        if (matches) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Cardholder name does not match customer",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
