package com.ksa.financing.fraud.infrastructure.rule.geoaccess;

import com.ksa.financing.fraud.domain.model.device.DeviceIntegrityStatus;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import org.springframework.stereotype.Component;

/**
 * ACC_002 — Jailbroken or Rooted Device.
 * Trigger: device_integrity = JAILBROKEN | ROOTED.
 */
@Component
public class JailbrokenRootedDeviceEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.ACC_002;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.deviceInfo() == null || event.deviceInfo().integrityStatus() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var status = event.deviceInfo().integrityStatus();
        if (status != DeviceIntegrityStatus.JAILBROKEN && status != DeviceIntegrityStatus.ROOTED) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Compromised device: " + status.name(),
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
