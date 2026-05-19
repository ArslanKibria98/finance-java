package com.ksa.financing.notification.infrastructure.external;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class NovuClient {

    private final RestClient restClient;
    private final String apiKey;

    public NovuClient(@Value("${novu.api-key}") String apiKey,
                      @Value("${novu.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "ApiKey " + apiKey)
                .build();
    }

    public void triggerEvent(String templateId, String subscriberId, Map<String, Object> payload, String locale) {
        log.info("Triggering Novu event: templateId={}, subscriberId={}, locale={}", templateId, subscriberId, locale);

        TriggerRequest request = TriggerRequest.builder()
                .name(templateId)
                .to(Subscriber.builder()
                        .subscriberId(subscriberId)
                        .locale(locale)
                        .build())
                .payload(payload)
                .build();

        try {
            restClient.post()
                    .uri("/events/trigger")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            
            log.debug("Successfully triggered Novu event for subscriber: {}", subscriberId);
        } catch (Exception e) {
            log.error("Failed to trigger Novu event: {}", e.getMessage(), e);
            throw new RuntimeException("Novu trigger failed", e);
        }
    }

    public void setSubscriberCredentials(String subscriberId, String fcmToken) {
        log.info("Updating FCM token for subscriber: {}", subscriberId);

        Map<String, Object> credentials = Map.of(
                "deviceTokens", new String[]{fcmToken}
        );

        Map<String, Object> request = Map.of(
                "providerId", "fcm",
                "credentials", credentials
        );

        try {
            restClient.put()
                    .uri("/subscribers/{subscriberId}/credentials", subscriberId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            
            log.debug("Successfully updated FCM token for subscriber: {}", subscriberId);
        } catch (Exception e) {
            log.error("Failed to update Novu credentials: {}", e.getMessage());
        }
    }

    public Map<String, Object> getWorkflows() {
        log.info("Fetching workflows from Novu (v2 API)");
        try {
            RestClient v2Client = RestClient.builder()
                    .baseUrl("https://api.novu.co/v2")
                    .defaultHeader("Authorization", "ApiKey " + apiKey)
                    .build();

            Map<String, Object> raw = v2Client.get()
                    .uri("/workflows?limit=100")
                    .retrieve()
                    .body(Map.class);

            if (raw == null || raw.get("data") == null) {
                return Map.of("data", java.util.Collections.emptyList());
            }

            Map<String, Object> data = (Map<String, Object>) raw.get("data");
            Object workflows = data.getOrDefault("workflows", java.util.Collections.emptyList());
            Object totalCount = data.getOrDefault("totalCount", 0);

            return Map.of(
                    "data", workflows,
                    "totalCount", totalCount
            );
        } catch (Exception e) {
            log.error("Failed to fetch workflows from Novu: {}", e.getMessage());
            return Map.of("data", java.util.Collections.emptyList());
        }
    }

    @Data
    @Builder
    public static class TriggerRequest {
        private String name;
        private Subscriber to;
        private Map<String, Object> payload;
    }

    @Data
    @Builder
    public static class Subscriber {
        private String subscriberId;
        private String locale;
        private String email;
        private String phone;
        private String firstName;
        private String lastName;
    }
}
