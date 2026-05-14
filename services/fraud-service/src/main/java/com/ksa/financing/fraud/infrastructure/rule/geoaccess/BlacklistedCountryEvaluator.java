package com.ksa.financing.fraud.infrastructure.rule.geoaccess;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.CountryBlacklistRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * GEO_001 — Blacklisted Country Access.
 * Trigger: resolved country (IP/GPS) is in active country_blacklist.
 */
@Component
@RequiredArgsConstructor
public class BlacklistedCountryEvaluator implements FraudRuleEvaluator {

    private final CountryBlacklistRepository countryBlacklistRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.GEO_001;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        String country = resolveCountry(event);
        if (country == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        if (!countryBlacklistRepository.isBlacklisted(event.tenantId(), country)) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Access from blacklisted country: " + country,
                EvaluatorScores.forDecision(rule.defaultAction()));
    }

    private String resolveCountry(FraudEvent event) {
        if (event.resolvedCountry() != null) return event.resolvedCountry();
        if (event.locationData() != null) {
            if (event.locationData().gpsCountry() != null) return event.locationData().gpsCountry();
            if (event.locationData().ipCountry() != null) return event.locationData().ipCountry();
        }
        return null;
    }
}
