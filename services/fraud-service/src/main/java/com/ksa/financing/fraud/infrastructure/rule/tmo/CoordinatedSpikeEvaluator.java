package com.ksa.financing.fraud.infrastructure.rule.tmo;

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

import java.util.HashSet;

/**
 * TMO_017 — Coordinated Application Spike (fraud ring).
 * Trigger: 3+ apps from different customers sharing device_id, disbursement_iban, ip_address within 24h.
 */
@Component
@RequiredArgsConstructor
public class CoordinatedSpikeEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_017;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int min = rule.getParameterInt("min_shared_applications", 3);
        int windowHours = rule.getParameterInt("time_window_hours", 24);
        var since = event.eventTimestamp().minusHours(windowHours);

        var distinct = new HashSet<String>();

        if (event.deviceInfo() != null && event.deviceInfo().deviceId() != null) {
            for (var e : fraudEventRepository.findByDeviceId(event.deviceInfo().deviceId(), since)) {
                if (e.eventType() == FraudEventType.LOAN_APPLICATION && e.customerId() != null) {
                    distinct.add(e.customerId());
                }
            }
        }
        if (event.disbursementIban() != null) {
            for (var e : fraudEventRepository.findByDisbursementIban(event.disbursementIban(), since)) {
                if (e.eventType() == FraudEventType.LOAN_APPLICATION && e.customerId() != null) {
                    distinct.add(e.customerId());
                }
            }
        }
        if (event.locationData() != null && event.locationData().ipAddress() != null) {
            for (var e : fraudEventRepository.findByIpAddress(event.locationData().ipAddress(), since)) {
                if (e.eventType() == FraudEventType.LOAN_APPLICATION && e.customerId() != null) {
                    distinct.add(e.customerId());
                }
            }
        }
        distinct.add(event.customerId());

        if (distinct.size() < min) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                distinct.size() + " distinct customers share attributes in " + windowHours + "h",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
