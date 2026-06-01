package com.ksa.financing.risk.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaBlacklistEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.blacklist-added:financing.blacklist.added}")
    private String blacklistAddedTopic;

    public void publishBlacklisted(String blacklistType, String identifier, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "BLACKLIST_ADDED");
        event.put("tenantId", "00000000-0000-0000-0000-000000000001");
        event.put("blacklistType", blacklistType);
        event.put("identifier", identifier);
        event.put("reason", reason);
        event.put("occurredAt", Instant.now().toString());

        log.info("Publishing BLACKLIST_ADDED event type={} identifier={}", blacklistType, mask(identifier));
        kafkaTemplate.send(blacklistAddedTopic, identifier, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish blacklist event: {}", ex.getMessage(), ex);
                    } else {
                        log.debug("Blacklist event published to {}", blacklistAddedTopic);
                    }
                });
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
}
