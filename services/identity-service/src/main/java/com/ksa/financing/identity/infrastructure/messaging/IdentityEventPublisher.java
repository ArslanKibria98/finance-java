package com.ksa.financing.identity.infrastructure.messaging;

import com.ksa.financing.identity.domain.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityEventPublisher implements EventPublisherPort {

    private static final String TOPIC_USER_REGISTERED = "islamic-financing.identity.user-registered";
    private static final String TOPIC_USER_STATUS_CHANGED = "islamic-financing.identity.user-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishUserRegistered(UUID userId, UUID tenantId, String fcmToken) {
        try {
            log.info("Publishing user-registered event for userId: {}, tenantId: {}", userId, tenantId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("tenantId", tenantId.toString());
            if (fcmToken != null) {
                payload.put("fcmToken", fcmToken);
            }

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_REGISTERED");
            event.put("timestamp", Instant.now().toString());
            event.put("payload", payload);

            kafkaTemplate.send(TOPIC_USER_REGISTERED, userId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish user-registered event for userId: {}. Error: {}",
                                    userId, ex.getMessage());
                        } else {
                            log.debug("User-registered event published successfully for userId: {}, offset: {}",
                                    userId, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka unavailable — skipping user-registered event for userId: {}. Error: {}", userId, e.getMessage());
        }
    }

    @Override
    public void publishUserStatusChanged(UUID userId, String fromStatus, String toStatus) {
        try {
            log.info("Publishing user-status-changed event for userId: {}, {} -> {}", userId, fromStatus, toStatus);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("fromStatus", fromStatus);
            payload.put("toStatus", toStatus);

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_STATUS_CHANGED");
            event.put("timestamp", Instant.now().toString());
            event.put("payload", payload);

            kafkaTemplate.send(TOPIC_USER_STATUS_CHANGED, userId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish user-status-changed event for userId: {}. Error: {}",
                                    userId, ex.getMessage());
                        } else {
                            log.debug("User-status-changed event published successfully for userId: {}, offset: {}",
                                    userId, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka unavailable — skipping user-status-changed event for userId: {}. Error: {}", userId, e.getMessage());
        }
    }
}
