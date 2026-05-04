package com.ksa.financing.fraud.infrastructure.rule.device;

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
 * DEV_001 — Multiple Accounts on Same Device.
 * Trigger: 3+ distinct accounts on same device_id within 48 hours.
 */
@Component
@RequiredArgsConstructor
public class MultipleAccountsSameDeviceEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.DEV_001;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.ONBOARDING
                && event.eventType() != FraudEventType.ACCOUNT_UPDATE) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (event.deviceInfo() == null || event.deviceInfo().deviceId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int maxAccounts = rule.getParameterInt("max_accounts", 3);
        int windowHours = rule.getParameterInt("time_window_hours", 48);

        long count = fraudEventRepository.countDistinctCustomersByDeviceId(
                event.deviceInfo().deviceId(),
                event.eventTimestamp().minusHours(windowHours));
        if (count < maxAccounts) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                count + " distinct accounts on device in " + windowHours + "h",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
