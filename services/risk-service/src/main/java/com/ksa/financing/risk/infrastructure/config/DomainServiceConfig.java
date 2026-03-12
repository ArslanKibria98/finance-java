package com.ksa.financing.risk.infrastructure.config;

import com.ksa.financing.risk.domain.service.AmlRiskScoringEngine;
import com.ksa.financing.risk.domain.service.CreditScoringDecisionEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public CreditScoringDecisionEngine creditScoringDecisionEngine() {
        return new CreditScoringDecisionEngine();
    }
}
