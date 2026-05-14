package com.ksa.financing.ledger.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the ledger-service Fineract proxy.
 * Bind via {@code @EnableConfigurationProperties(LedgerClientProperties.class)}
 * or rely on {@link LedgerFineractClientAutoConfiguration}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ksa.ledger.client")
public class LedgerClientProperties {

    /** Base URL of ledger-service, e.g. {@code http://ledger-service:8095}. */
    private String baseUrl = "http://localhost:8095";

    /** Caller-service identifier sent in the {@code X-Caller-Service} header. */
    private String callerService = "unknown";

    /** Whether to use the JWT-authenticated /api/v1 endpoints (true) or /internal (false). */
    private boolean useAuthenticatedEndpoints = false;

    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}
