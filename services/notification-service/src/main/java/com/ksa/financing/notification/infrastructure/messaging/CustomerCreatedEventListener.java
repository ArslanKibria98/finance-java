package com.ksa.financing.notification.infrastructure.messaging;

import com.ksa.financing.notification.application.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerCreatedEventListener {

    private final NotificationPreferenceService preferenceService;

    @KafkaListener(
            topics = "${kafka.topics.customer-created:islamic-financing.customer.customer-created}",
            groupId = "${spring.application.name}-prefs"
    )
    public void onCustomerCreated(@Payload Map<String, Object> message,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received customer-created event topic={} payload={}", topic, message);

        try {
            Object payloadObj = message.get("payload");
            if (!(payloadObj instanceof Map<?, ?> payload)) {
                log.warn("Invalid customer-created event — missing payload. Skipping.");
                return;
            }

            UUID tenantId = extractUuid(payload, "tenantId");
            UUID customerId = extractUuid(payload, "customerId");

            if (tenantId == null || customerId == null) {
                log.warn("Missing tenantId/customerId in customer-created event. Skipping. payload={}", payload);
                return;
            }

            String preferredLanguage = extractString(payload, "preferredLanguage");
            preferenceService.createDefault(tenantId, customerId, preferredLanguage);

        } catch (Exception e) {
            log.error("Error processing customer-created event: {}", e.getMessage(), e);
        }
    }

    private UUID extractUuid(Map<?, ?> payload, String key) {
        Object val = payload.get(key);
        if (val == null) return null;
        try {
            return UUID.fromString(val.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for key '{}': {}", key, val);
            return null;
        }
    }

    private String extractString(Map<?, ?> payload, String key) {
        Object val = payload.get(key);
        return val != null ? val.toString() : null;
    }
}
