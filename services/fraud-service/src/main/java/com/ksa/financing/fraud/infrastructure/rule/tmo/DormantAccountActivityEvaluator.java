package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudUserProfileRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * TMO_012 — Dormant Account Sudden Transaction Activity.
 * Trigger: account inactive > 90 days then submits app or login.
 */
@Component
@RequiredArgsConstructor
public class DormantAccountActivityEvaluator implements FraudRuleEvaluator {

    private final FraudUserProfileRepository fraudUserProfileRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_012;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION
                && event.eventType() != FraudEventType.LOGIN) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int dormantDays = rule.getParameterInt("dormant_days", 90);
        var profile = fraudUserProfileRepository.findByCustomerId(event.tenantId(), event.customerId());
        if (profile.isEmpty() || profile.get().lastActivityAt() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        long daysSince = Duration.between(profile.get().lastActivityAt(), event.eventTimestamp()).toDays();
        if (daysSince < dormantDays) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Account dormant for " + daysSince + " days",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
