package com.ksa.financing.notification.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Small REST client used by notification-service to resolve the correct Novu push
 * integration ({@code fcm} vs {@code firebase-cloud-messaging-for-sullis}) for a
 * given customer. The decision is made on the identity-service side, based on the
 * Keycloak {@code onboarding_flow} attribute (Canada / Foreign / Guest → Sullis).
 */
@Component
@Slf4j
public class IdentityServiceClient {

    private static final String DEFAULT_PROVIDER = "fcm";

    private final RestClient restClient;

    public IdentityServiceClient(@Value("${app.services.identity-service-url}") String identityServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(identityServiceUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public String resolveNotificationProvider(String customerId) {
        if (customerId == null || customerId.isBlank()) return DEFAULT_PROVIDER;
        try {
            Map<String, Object> envelope = restClient.get()
                    .uri("/internal/users/{id}/notification-provider", customerId)
                    .retrieve()
                    .body(Map.class);
            if (envelope == null) return DEFAULT_PROVIDER;
            // Foundational-infra wraps responses as {"data": {...}, "message": "success", ...}.
            Object data = envelope.get("data");
            Map<String, Object> payload = data instanceof Map ? (Map<String, Object>) data : envelope;
            Object provider = payload.get("providerId");
            return provider != null ? provider.toString() : DEFAULT_PROVIDER;
        } catch (Exception e) {
            log.warn("identity-service notification-provider lookup failed for customerId={} — defaulting to {}: {}",
                    customerId, DEFAULT_PROVIDER, e.getMessage());
            return DEFAULT_PROVIDER;
        }
    }
}
