package com.ksa.islamic.messaging.kafka.producer;

import com.ksa.islamic.messaging.contract.EventEnvelope;
import com.ksa.islamic.messaging.exception.EventPublishingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Domain Event Producer for publishing domain events with event envelope
 * Implements idempotent producer pattern with monitoring
 */
@Slf4j
@Component
public class DomainEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer publishTimer;

    public DomainEventProducer(
            @Qualifier("avroKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;

        // Initialize metrics
        this.successCounter = Counter.builder("domain.events.published.success")
            .description("Number of successfully published domain events")
            .register(meterRegistry);

        this.failureCounter = Counter.builder("domain.events.published.failure")
            .description("Number of failed domain event publications")
            .register(meterRegistry);

        this.publishTimer = Timer.builder("domain.events.publish.duration")
            .description("Duration of domain event publishing")
            .register(meterRegistry);
    }

    /**
     * Publish a domain event with automatic envelope wrapping
     *
     * @param topicName The topic to publish to (follows pattern: domain.<context>.<aggregate>.<event>)
     * @param event The event payload
     * @param tenantId The tenant ID
     * @param correlationId Optional correlation ID for tracing
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, Object>> publishEvent(
            String topicName,
            Object event,
            String tenantId,
            String correlationId) {

        return publishTimer.recordCallable(() -> {
            try {
                // Create event envelope
                EventEnvelope envelope = createEventEnvelope(event, tenantId, correlationId);

                // Create producer record with headers
                ProducerRecord<String, Object> record = createProducerRecord(
                    topicName, envelope, tenantId, correlationId
                );

                log.info("Publishing domain event to topic: {}, EventType: {}, " +
                        "TenantId: {}, CorrelationId: {}, EventId: {}",
                        topicName, event.getClass().getSimpleName(),
                        tenantId, correlationId, envelope.getEventId());

                // Send the event
                CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

                // Handle completion
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        failureCounter.increment();
                        log.error("Failed to publish event to topic: {}, Error: {}",
                                topicName, ex.getMessage(), ex);
                    } else {
                        successCounter.increment();
                        log.debug("Successfully published event to topic: {}, " +
                                "Partition: {}, Offset: {}",
                                topicName,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

                return future;

            } catch (Exception e) {
                failureCounter.increment();
                log.error("Error preparing event for publication: {}", e.getMessage(), e);
                CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(
                    new EventPublishingException("Failed to publish event", e)
                );
                return failedFuture;
            }
        });
    }

    /**
     * Publish event with custom headers
     */
    public CompletableFuture<SendResult<String, Object>> publishEventWithHeaders(
            String topicName,
            Object event,
            String tenantId,
            String correlationId,
            Headers customHeaders) {

        EventEnvelope envelope = createEventEnvelope(event, tenantId, correlationId);
        ProducerRecord<String, Object> record = createProducerRecord(
            topicName, envelope, tenantId, correlationId
        );

        // Add custom headers
        if (customHeaders != null) {
            customHeaders.forEach(header ->
                record.headers().add(header.key(), header.value())
            );
        }

        return kafkaTemplate.send(record);
    }

    /**
     * Publish event with specific partition key
     */
    public CompletableFuture<SendResult<String, Object>> publishEventWithKey(
            String topicName,
            String key,
            Object event,
            String tenantId,
            String correlationId) {

        EventEnvelope envelope = createEventEnvelope(event, tenantId, correlationId);

        ProducerRecord<String, Object> record = new ProducerRecord<>(
            topicName,
            null, // partition
            System.currentTimeMillis(),
            key, // Use provided key for partitioning
            envelope
        );

        // Add standard headers
        addStandardHeaders(record.headers(), tenantId, correlationId, envelope.getEventId());

        log.info("Publishing event with key: {} to topic: {}", key, topicName);

        return kafkaTemplate.send(record);
    }

    /**
     * Publish event synchronously with timeout
     */
    public SendResult<String, Object> publishEventSync(
            String topicName,
            Object event,
            String tenantId,
            String correlationId,
            long timeoutSeconds) throws EventPublishingException {

        try {
            CompletableFuture<SendResult<String, Object>> future = publishEvent(
                topicName, event, tenantId, correlationId
            );

            return future.get(timeoutSeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new EventPublishingException(
                "Failed to publish event synchronously within timeout", e
            );
        }
    }

    /**
     * Create event envelope wrapper
     */
    private EventEnvelope createEventEnvelope(Object event, String tenantId, String correlationId) {
        return EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(event.getClass().getSimpleName())
            .eventVersion("1.0")
            .tenantId(tenantId)
            .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
            .causationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .payload(event)
            .build();
    }

    /**
     * Create producer record with headers
     */
    private ProducerRecord<String, Object> createProducerRecord(
            String topicName,
            EventEnvelope envelope,
            String tenantId,
            String correlationId) {

        // Use tenant ID as partition key for tenant isolation
        String key = tenantId;

        ProducerRecord<String, Object> record = new ProducerRecord<>(
            topicName,
            null, // Let Kafka decide partition based on key
            System.currentTimeMillis(),
            key,
            envelope
        );

        // Add headers
        addStandardHeaders(record.headers(), tenantId, correlationId, envelope.getEventId());

        return record;
    }

    /**
     * Add standard headers to the record
     */
    private void addStandardHeaders(Headers headers, String tenantId, String correlationId, String eventId) {
        headers.add("tenant.id", tenantId.getBytes(StandardCharsets.UTF_8));
        headers.add(KafkaHeaders.CORRELATION_ID,
                   (correlationId != null ? correlationId : UUID.randomUUID().toString())
                   .getBytes(StandardCharsets.UTF_8));
        headers.add("event.id", eventId.getBytes(StandardCharsets.UTF_8));
        headers.add("timestamp", Instant.now().toString().getBytes(StandardCharsets.UTF_8));
        headers.add("source.service", "islamic-financing-platform".getBytes(StandardCharsets.UTF_8));
        headers.add("pod.name",
                   System.getProperty("POD_NAME", "local").getBytes(StandardCharsets.UTF_8));
    }
}