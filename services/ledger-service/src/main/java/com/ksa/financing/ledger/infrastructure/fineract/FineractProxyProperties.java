package com.ksa.financing.ledger.infrastructure.fineract;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Centralised Fineract connection properties for ledger-service.
 * Mirrors the {@code app.fineract.*} block in application.yml.
 * <p>
 * Ledger-service is the ONLY service that should hold Fineract credentials.
 * All other services route through ledger-service proxy endpoints.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.fineract")
public class FineractProxyProperties {

    private String baseUrl = "https://localhost:8443/fineract-provider/api/v1";
    private String tenantId = "default";
    private String username = "mifos";
    private String password = "password";
    private int officeId = 1;
    private int defaultSavingsProductId = 1;
    private int idempotencyTtlHours = 24;
}
