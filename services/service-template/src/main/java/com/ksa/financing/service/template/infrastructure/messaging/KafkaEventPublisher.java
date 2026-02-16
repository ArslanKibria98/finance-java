package com.ksa.financing.service.template.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ksa.financing.domain.base.DomainEvent;
import com.ksa.financing.messaging.event.EventEnvelope;
import com.ksa.financing.messaging.event.EventMetadata;
import com.ksa.financing.service.template.domain.port.out.EventPublisher;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka implementation of EventPublisher.
 * Publishes domain events to Kafka topics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic naming convention
    private static final String TOPIC_PREFIX = "islamic-financing.";
    private static final String SERVICE_NAME = "service-template";

    @Override
    public void publish(DomainEvent event) {
        try {
            var topic = buildTopicName(event);
            var envelope = wrapInEnvelope(event);

            log.debug("Publishing event to topic: {}", topic);

            kafkaTemplate.send(topic, envelope.getEventId(), envelope)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Event published successfully: {}", event.getClass().getSimpleName());
                        } else {
                            log.error("Failed to publish event: {}", event.getClass().getSimpleName(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing event: {}", event.getClass().getSimpleName(), e);
            // In production, might want to store failed events for retry
        }
    }

    @Override
    public void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }

    /**
     * Build topic name from event type.
     * Example: islamic-financing.service-template.example-aggregate-created
     */
    private String buildTopicName(DomainEvent event) {
        var eventName = event.getClass().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();

        return TOPIC_PREFIX + SERVICE_NAME + "." + eventName;
    }

    /**
     * Wrap domain event in standard envelope with metadata.
     */
    private EventEnvelope wrapInEnvelope(DomainEvent event) {
        var metadata = EventMetadata.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(event.getClass().getName())
                .source(SERVICE_NAME)
                .timestamp(Instant.now())
                .correlationId(getCorrelationId())
                .build();

        return EventEnvelope.builder()
                .eventId(metadata.getEventId())
                .eventType(metadata.getEventType())
                .metadata(metadata)
                .payload(event)
                .build();
    }

    /**
     * Get correlation ID from MDC or generate new one.
     */
    private String getCorrelationId() {
        // In production, get from MDC (Mapped Diagnostic Context)
        return UUID.randomUUID().toString();
    }
}