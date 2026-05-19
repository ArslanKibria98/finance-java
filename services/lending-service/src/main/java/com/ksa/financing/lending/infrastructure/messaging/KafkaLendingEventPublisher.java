package com.ksa.financing.lending.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaLendingEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_PREFIX = "financing.loan.";

    private static final Map<String, String> EVENT_TYPE_MAP = Map.of(
            "LoanApplicationApproved", "LOAN_APPROVED",
            "LoanDisbursed",           "LOAN_DISBURSED",
            "LoanRescheduled",         "LOAN_RESCHEDULED",
            "LoanSettled",             "LOAN_SETTLED",
            "LoanCreated",             "LOAN_CREATED",
            "LoanApplicationRejected", "LOAN_REJECTED"
    );

    @Override
    public void publish(Object event) {
        try {
            var topic = buildTopicName(event);
            var key = UUID.randomUUID().toString();
            var payload = enrichWithEventType(event);

            log.debug("Publishing event to topic: {}, type: {}", topic, event.getClass().getSimpleName());

            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Event published successfully: {}", event.getClass().getSimpleName());
                        } else {
                            log.error("Failed to publish event: {}", event.getClass().getSimpleName(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event: {}", event.getClass().getSimpleName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enrichWithEventType(Object event) {
        Map<String, Object> map = objectMapper.convertValue(event, Map.class);
        Map<String, Object> enriched = new HashMap<>(map);
        String className = event.getClass().getSimpleName();
        enriched.putIfAbsent("eventType", EVENT_TYPE_MAP.getOrDefault(className, className));
        return enriched;
    }

    @Override
    public void publishAll(List<Object> events) {
        events.forEach(this::publish);
    }

    private String buildTopicName(Object event) {
        var eventName = event.getClass().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
        return TOPIC_PREFIX + eventName;
    }
}
