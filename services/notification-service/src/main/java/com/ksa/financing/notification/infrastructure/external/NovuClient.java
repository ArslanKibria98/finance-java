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

    /**
     * Create or update (upsert) a Novu subscriber's identity fields so Novu's email/SMS
     * channel steps have a recipient address. Idempotent — Novu's identify endpoint creates
     * the subscriber if missing, otherwise patches the supplied fields.
     *
     * <p>Driven by {@code customer-created}/{@code customer-updated} events. {@code subscriberId}
     * MUST be the {@code customerId} (same key the trigger flow targets).</p>
     */
    public void upsertSubscriber(String subscriberId, String email, String phone,
                                 String firstName, String lastName) {
        if (subscriberId == null || subscriberId.isBlank()) {
            log.warn("upsertSubscriber called without subscriberId — skipping");
            return;
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("subscriberId", subscriberId);
        if (email != null && !email.isBlank()) body.put("email", email);
        if (phone != null && !phone.isBlank()) body.put("phone", phone);
        if (firstName != null && !firstName.isBlank()) body.put("firstName", firstName);
        if (lastName != null && !lastName.isBlank()) body.put("lastName", lastName);

        // Novu's POST /subscribers ONLY creates — it does NOT update an existing
        // subscriber's fields (silently 409). So we ALWAYS follow with PUT to make
        // sure email / name updates land. PUT body excludes `subscriberId` (it's in
        // the path).
        boolean created = false;
        try {
            restClient.post()
                    .uri("/subscribers")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            created = true;
            log.info("Created Novu subscriber {} (email set={})", subscriberId, email != null && !email.isBlank());
        } catch (Exception e) {
            // 409 / "already exists" is expected — proceed to PUT
            log.debug("POST /subscribers for {} did not create (likely exists): {}", subscriberId, e.getMessage());
        }

        // Update existing subscriber so email / name actually persist.
        Map<String, Object> putBody = new java.util.HashMap<>(body);
        putBody.remove("subscriberId");
        if (putBody.isEmpty()) return;
        try {
            restClient.put()
                    .uri("/subscribers/{subscriberId}", subscriberId)
                    .body(putBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Updated Novu subscriber {} (created={}, fields={})", subscriberId, created, putBody.keySet());
        } catch (Exception e) {
            log.error("Failed to PUT Novu subscriber {}: {}", subscriberId, e.getMessage());
        }
    }

    /** Legacy single-app call — defaults to the {@code fcm} integration. */
    public void setSubscriberCredentials(String subscriberId, String fcmToken) {
        setSubscriberCredentials(subscriberId, fcmToken, "fcm");
    }

    /**
     * Update the FCM credentials for a subscriber on a specific Novu push integration.
     *
     * <p>Novu's {@code PUT /subscribers/{id}/credentials} only accepts the provider
     * <b>type</b> ({@code fcm}, {@code apns}, …) in the {@code providerId} field — it
     * rejects custom integration identifiers like {@code firebase-cloud-messaging-for-sullis}
     * with {@code 422 "providerId must be a valid provider ID"}. To target a specific
     * integration instance when multiple {@code fcm} integrations exist (e.g. main vs
     * Sullis app), we pass the integration's {@code identifier} in the
     * {@code integrationIdentifier} field instead.</p>
     *
     * @param subscriberId subscriber (customerId)
     * @param fcmToken     FCM device token
     * @param providerId   integration identifier (e.g. {@code firebase-cloud-messaging-for-sullis})
     *                     OR a Novu provider type ({@code fcm}, {@code apns}). Treated as
     *                     {@code integrationIdentifier} when not a recognised type.
     */
    public void setSubscriberCredentials(String subscriberId, String fcmToken, String providerId) {
        // Novu accepts these as raw providerId values. Anything else is treated as
        // an integration identifier and the actual providerId is forced to "fcm".
        java.util.Set<String> validTypes = java.util.Set.of("fcm", "apns", "expo", "one_signal", "pushpad", "push_webhook");
        String resolvedType = "fcm";
        String integrationIdentifier = null;
        if (providerId != null && !providerId.isBlank()) {
            if (validTypes.contains(providerId)) {
                resolvedType = providerId;
            } else {
                integrationIdentifier = providerId;
            }
        }
        log.info("Updating FCM token for subscriber: {} providerId={} integrationIdentifier={}",
                subscriberId, resolvedType, integrationIdentifier);

        Map<String, Object> credentials = Map.of(
                "deviceTokens", new String[]{fcmToken}
        );

        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("providerId", resolvedType);
        request.put("credentials", credentials);
        if (integrationIdentifier != null) {
            request.put("integrationIdentifier", integrationIdentifier);
        }

        try {
            restClient.put()
                    .uri("/subscribers/{subscriberId}/credentials", subscriberId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully updated FCM token for subscriber: {} providerId={} integration={}",
                    subscriberId, resolvedType, integrationIdentifier);
        } catch (Exception e) {
            log.error("Failed to update Novu credentials providerId={} integration={}: {}",
                    resolvedType, integrationIdentifier, e.getMessage());
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

    /**
     * Subscriber payload for {@code POST /events/trigger}.
     *
     * <p>Novu's trigger endpoint upserts the subscriber from this {@code to} object —
     * any field present here OVERWRITES the stored value, including setting null
     * over an existing email. {@code @JsonInclude(NON_NULL)} keeps unset fields out
     * of the JSON entirely so the trigger doesn't clobber the email/name that
     * {@link NovuClient#upsertSubscriber} just put in place.</p>
     */
    @Data
    @Builder
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class Subscriber {
        private String subscriberId;
        private String locale;
        private String email;
        private String phone;
        private String firstName;
        private String lastName;
    }
}
