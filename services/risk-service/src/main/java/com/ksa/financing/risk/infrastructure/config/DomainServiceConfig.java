package com.ksa.financing.risk.infrastructure.config;

import com.ksa.financing.risk.domain.service.AmlRiskScoringEngine;
import com.ksa.financing.risk.domain.service.FraudDecisionEngine;
import com.ksa.financing.risk.domain.service.FraudRuleEngine;
import com.ksa.financing.risk.domain.service.FraudRuleEvaluator;
import com.ksa.financing.risk.domain.service.GeoDistanceCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring configuration for pure domain services that need to be managed as beans.
 * Domain services have zero framework imports, so they need explicit bean registration.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public AmlRiskScoringEngine amlRiskScoringEngine() {
        return new AmlRiskScoringEngine();
    }

    @Bean
    public FraudRuleEngine fraudRuleEngine(List<FraudRuleEvaluator> evaluators) {
        Map<String, FraudRuleEvaluator> evaluatorMap = evaluators.stream()
                .collect(Collectors.toMap(
                        e -> e.getRuleId().name(),
                        Function.identity()
                ));
        return new FraudRuleEngine(evaluatorMap);
    }

    @Bean
    public FraudDecisionEngine fraudDecisionEngine() {
        return new FraudDecisionEngine();
    }

    @Bean
    public GeoDistanceCalculator geoDistanceCalculator() {
        return new GeoDistanceCalculator();
    }
}
