package com.ksa.financing.fraud.infrastructure.rule.geoaccess;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import org.springframework.stereotype.Component;

/**
 * ACC_001 — VPN or Proxy Usage Detected.
 * Trigger: event.vpnDetected OR event.proxyDetected = true.
 */
@Component
public class VpnProxyEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.ACC_001;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        boolean vpn = event.vpnDetected()
                || (event.locationData() != null && event.locationData().vpnDetected());
        boolean proxy = event.proxyDetected()
                || (event.locationData() != null && event.locationData().proxyDetected());
        if (!vpn && !proxy) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var detail = vpn && proxy ? "VPN+Proxy detected" : (vpn ? "VPN detected" : "Proxy detected");
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                detail, EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
