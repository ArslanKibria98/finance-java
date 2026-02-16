package com.ksa.financing.lms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * Configuration properties for Apache Fineract integration.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "fineract")
public class FineractConfig {

    @NotBlank
    private String baseUrl = "https://localhost:8443/fineract-provider/api/v1";

    @NotBlank
    private String username = "mifos";

    @NotBlank
    private String password = "password";

    @NotBlank
    private String tenantId = "default";

    @NotNull
    @Min(1000)
    private Integer connectTimeoutMs = 5000;

    @NotNull
    @Min(1000)
    private Integer readTimeoutMs = 30000;

    @NotNull
    private Integer maxRetries = 3;

    @NotNull
    @Min(100)
    private Integer retryDelayMs = 1000;

    private boolean sslValidation = false;

    // Pool configuration
    @NotNull
    @Min(1)
    private Integer maxConnections = 100;

    @NotNull
    @Min(1)
    private Integer maxConnectionsPerRoute = 20;

    // Circuit breaker configuration
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    @Data
    public static class CircuitBreakerConfig {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private int requestVolumeThreshold = 20;
        private int sleepWindowMs = 60000;
        private int timeoutMs = 10000;
    }
}