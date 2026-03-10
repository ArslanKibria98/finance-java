package com.ksa.financing.lending.infrastructure.messaging;

import com.ksa.financing.lending.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaLendingEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PREFIX = "financing.loan.";

    @Override
    public void publish(Object event) {
        try {
            var topic = buildTopicName(event);
            var key = UUID.randomUUID().toString();

            log.debug("Publishing event to topic: {}, type: {}", topic, event.getClass().getSimpleName());

            kafkaTemplate.send(topic, key, event)
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
