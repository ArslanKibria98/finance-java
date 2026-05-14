package com.ksa.financing.fraud.infrastructure.rule.location;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.SessionEventRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.domain.service.GeoDistanceCalculator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * LOC_002 — Sudden Location Change (Same Device — GPS spoof signal).
 * Trigger: same device_id + distance > 200 km + within 1 hour.
 */
@Component
@RequiredArgsConstructor
public class SuddenLocationChangeEvaluator implements FraudRuleEvaluator {

    private final SessionEventRepository sessionEventRepository;
    private final GeoDistanceCalculator geoDistanceCalculator;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.LOC_002;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.locationData() == null
                || event.locationData().latitude() == null
                || event.deviceInfo() == null
                || event.deviceInfo().deviceId() == null
                || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int distanceKm = rule.getParameterInt("distance_km", 200);
        int windowHours = rule.getParameterInt("time_window_hours", 1);

        var prev = sessionEventRepository.findLastByCustomerAndDevice(
                event.tenantId(), event.customerId(), event.deviceInfo().deviceId());
        if (prev.isEmpty() || prev.get().loginAt() == null
                || prev.get().loginAt().isBefore(event.eventTimestamp().minusHours(windowHours))) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        BigDecimal distance = geoDistanceCalculator.calculateDistanceKm(
                prev.get().latitude(), prev.get().longitude(),
                event.locationData().latitude(), event.locationData().longitude());
        if (distance.compareTo(BigDecimal.valueOf(distanceKm)) <= 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Same device + " + distance + " km move within " + windowHours + "h (possible GPS spoof)",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
