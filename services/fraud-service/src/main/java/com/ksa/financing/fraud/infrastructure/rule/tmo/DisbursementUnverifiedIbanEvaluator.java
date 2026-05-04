package com.ksa.financing.fraud.infrastructure.rule.tmo;

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
 * TMO_007 — Disbursement to Unverified or Mismatched IBAN.
 * Trigger: ibanVerificationStatus = UNVERIFIED OR ibanHolderName != customer.fullName.
 */
@Component
@RequiredArgsConstructor
public class DisbursementUnverifiedIbanEvaluator implements FraudRuleEvaluator {

    private final CustomerProfilePort customerProfilePort;
    private final NameSimilarityPort nameSimilarityPort;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_007;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.DISBURSEMENT) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        boolean unverified = "UNVERIFIED".equalsIgnoreCase(event.ibanVerificationStatus());
        if (unverified) {
            return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                    "IBAN unverified",
                    EvaluatorScores.forDecision(rule.defaultAction()));
        }
        if (event.ibanHolderName() == null || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var profile = customerProfilePort.fetchProfile(event.tenantId(), event.customerId());
        if (profile.isEmpty() || profile.get().fullName() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (nameSimilarityPort.matches(profile.get().fullName(), event.ibanHolderName(), 0.85)) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "IBAN holder name does not match customer",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
