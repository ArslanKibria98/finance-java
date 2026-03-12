package com.ksa.financing.fraud.infrastructure.config;

import com.ksa.financing.fraud.domain.service.FraudDecisionEngine;
import com.ksa.financing.fraud.domain.service.FraudRuleEngine;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.domain.service.GeoDistanceCalculator;
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
