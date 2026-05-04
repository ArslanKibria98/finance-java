package com.ksa.financing.fraud.infrastructure.rule.device;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.DeviceBlacklistRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DEV_003 — Blacklisted Device Registration / Login.
 * Trigger: device_id present in active device_blacklist.
 */
@Component
@RequiredArgsConstructor
public class BlacklistedDeviceEvaluator implements FraudRuleEvaluator {

    private final DeviceBlacklistRepository deviceBlacklistRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.DEV_003;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.deviceInfo() == null || event.deviceInfo().deviceId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (!deviceBlacklistRepository.isBlacklisted(event.tenantId(), event.deviceInfo().deviceId())) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Device blacklisted: " + event.deviceInfo().deviceId(),
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
