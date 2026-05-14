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
import java.time.LocalDateTime;

/**
 * LOC_001 — Unusual Location Change.
 * Trigger: new device + new location + distance > 200 km within 3 hours.
 */
@Component
@RequiredArgsConstructor
public class UnusualLocationChangeEvaluator implements FraudRuleEvaluator {

    private final SessionEventRepository sessionEventRepository;
    private final GeoDistanceCalculator geoDistanceCalculator;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.LOC_001;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.locationData() == null
                || event.locationData().latitude() == null
                || event.locationData().longitude() == null
                || event.deviceInfo() == null
                || event.deviceInfo().deviceId() == null
                || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int distanceKm = rule.getParameterInt("distance_km", 200);
        int windowHours = rule.getParameterInt("time_window_hours", 3);

        var lastSession = sessionEventRepository.findLastByCustomer(event.tenantId(), event.customerId());
        if (lastSession.isEmpty()) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var prev = lastSession.get();
        boolean newDevice = !event.deviceInfo().deviceId().equals(prev.deviceId());
        boolean withinWindow = prev.loginAt() != null
                && prev.loginAt().isAfter(event.eventTimestamp().minusHours(windowHours));

        if (!newDevice || !withinWindow) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        BigDecimal distance = geoDistanceCalculator.calculateDistanceKm(
                prev.latitude(), prev.longitude(),
                event.locationData().latitude(), event.locationData().longitude());
        if (distance.compareTo(BigDecimal.valueOf(distanceKm)) <= 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "New device + " + distance + " km move within " + windowHours + "h",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
