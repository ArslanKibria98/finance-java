package com.ksa.financing.fraud.infrastructure.rule.location;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.CustomerProfilePort;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.domain.service.GeoDistanceCalculator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * LOC_003 — National Address Mismatch on Application.
 * Trigger: loan application from device location > 200 km from registered national address.
 */
@Component
@RequiredArgsConstructor
public class NationalAddressMismatchEvaluator implements FraudRuleEvaluator {

    private final CustomerProfilePort customerProfilePort;
    private final GeoDistanceCalculator geoDistanceCalculator;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.LOC_003;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION
                || event.locationData() == null
                || event.locationData().latitude() == null
                || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int distanceKm = rule.getParameterInt("distance_km", 200);

        var profile = customerProfilePort.fetchProfile(event.tenantId(), event.customerId());
        if (profile.isEmpty()
                || profile.get().nationalAddressLatitude() == null
                || profile.get().nationalAddressLongitude() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        BigDecimal distance = geoDistanceCalculator.calculateDistanceKm(
                profile.get().nationalAddressLatitude(),
                profile.get().nationalAddressLongitude(),
                event.locationData().latitude(),
                event.locationData().longitude());
        if (distance.compareTo(BigDecimal.valueOf(distanceKm)) <= 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "App location " + distance + " km from national address",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
