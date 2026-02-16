package com.ksa.financing.service.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Service Template.
 *
 * This template demonstrates:
 * - Hexagonal Architecture (Ports & Adapters)
 * - Domain-Driven Design
 * - Multi-tenancy support
 * - Event-driven architecture with Kafka
 * - Workflow orchestration with Temporal
 * - OAuth2/OIDC security with Keycloak
 * - Observability with OpenTelemetry
 */
@SpringBootApplication(scanBasePackages = {
    "com.ksa.financing.service.template",
    "com.ksa.financing.infra",           // Foundational infrastructure SDK
    "com.ksa.financing.domain",          // Domain core SDK
    "com.ksa.financing.workflow",        // Workflow orchestration SDK
    "com.ksa.financing.messaging",       // Messaging/event SDK
    "com.ksa.financing.lms",            // LMS adapter SDK
    "com.ksa.financing.reporting",      // Reporting projection SDK
    "com.ksa.financing.compliance"      // Compliance localization SDK
})
@EnableJpaRepositories
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class ServiceTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceTemplateApplication.class, args);
    }
}