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

import java.util.HashSet;

/**
 * DEV_002 — Multiple Loan Applications on Same Device.
 * Trigger: 2+ distinct customers + 2+ loan applications on same device within 48h.
 */
@Component
@RequiredArgsConstructor
public class MultipleAppsSameDeviceEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.DEV_002;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (event.deviceInfo() == null || event.deviceInfo().deviceId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int maxCustomers = rule.getParameterInt("max_customers", 2);
        int maxApps = rule.getParameterInt("max_applications", 2);
        int windowHours = rule.getParameterInt("time_window_hours", 48);

        var since = event.eventTimestamp().minusHours(windowHours);
        var events = fraudEventRepository.findByDeviceId(event.deviceInfo().deviceId(), since);
        long appsCount = events.stream()
                .filter(e -> e.eventType() == FraudEventType.LOAN_APPLICATION)
                .count();
        var distinctCustomers = new HashSet<String>();
        events.forEach(e -> { if (e.customerId() != null) distinctCustomers.add(e.customerId()); });

        if (distinctCustomers.size() < maxCustomers || appsCount < maxApps) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                distinctCustomers.size() + " customers, " + appsCount + " apps on device in " + windowHours + "h",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
