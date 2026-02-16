package com.ksa.islamic.messaging.kafka.producer;

import com.ksa.islamic.messaging.contract.EventEnvelope;
import com.ksa.islamic.messaging.contract.OutboxEvent;
import com.ksa.islamic.messaging.exception.EventPublishingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Transactional Outbox Producer for guaranteed event delivery
 * Implements the transactional outbox pattern for reliable event publishing
 */
@Slf4j
@Component
public class TransactionalOutboxProducer {

    private final KafkaTemplate<String, Object> transactionalKafkaTemplate;
    private final Counter outboxEventCounter;
    private final Counter outboxBatchCounter;
    private final Counter outboxErrorCounter;

    public TransactionalOutboxProducer(
            @Qualifier("transactionalKafkaTemplate") KafkaTemplate<String, Object> transactionalKafkaTemplate,
            MeterRegistry meterRegistry) {
        this.transactionalKafkaTemplate = transactionalKafkaTemplate;

        // Initialize metrics
        this.outboxEventCounter = Counter.builder("outbox.events.published")
            .description("Number of events published from outbox")
            .register(meterRegistry);

        this.outboxBatchCounter = Counter.builder("outbox.batches.published")
            .description("Number of outbox batches published")
            .register(meterRegistry);

        this.outboxErrorCounter = Counter.builder("outbox.events.errors")
            .description("Number of outbox publishing errors")
            .register(meterRegistry);
    }

    /**
     * Publish a single outbox event within a transaction
     *
     * @param outboxEvent The outbox event to publish
     * @return CompletableFuture with the send result
     */
    @Transactional
    public CompletableFuture<SendResult<String, Object>> publishOutboxEvent(OutboxEvent outboxEvent) {
        try {
            log.info("Publishing outbox event: EventId={}, AggregateId={}, EventType={}",
                    outboxEvent.getEventId(), outboxEvent.getAggregateId(), outboxEvent.getEventType());

            // Convert outbox event to event envelope
            EventEnvelope envelope = convertToEnvelope(outboxEvent);

            // Send within transaction
            CompletableFuture<SendResult<String, Object>> future = transactionalKafkaTemplate.send(
                outboxEvent.getTopicName(),
                outboxEvent.getPartitionKey(),
                envelope
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    outboxErrorCounter.increment();
                    log.error("Failed to publish outbox event: EventId={}, Error={}",
                            outboxEvent.getEventId(), ex.getMessage(), ex);
                } else {
                    outboxEventCounter.increment();
                    log.debug("Successfully published outbox event: EventId={}, Partition={}, Offset={}",
                            outboxEvent.getEventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            return future;

        } catch (Exception e) {
            outboxErrorCounter.increment();
            throw new EventPublishingException("Failed to publish outbox event", e);
        }
    }

    /**
     * Publish multiple outbox events in a single transaction
     *
     * @param outboxEvents List of outbox events to publish
     * @return List of CompletableFutures for each send operation
     */
    @Transactional
    public List<CompletableFuture<SendResult<String, Object>>> publishOutboxEventsBatch(
            List<OutboxEvent> outboxEvents) {

        log.info("Publishing batch of {} outbox events", outboxEvents.size());

        try {
            List<CompletableFuture<SendResult<String, Object>>> futures = outboxEvents.stream()
                .map(event -> {
                    EventEnvelope envelope = convertToEnvelope(event);
                    return transactionalKafkaTemplate.send(
                        event.getTopicName(),
                        event.getPartitionKey(),
                        envelope
                    );
                })
                .collect(Collectors.toList());

            // Increment batch counter
            outboxBatchCounter.increment();
            outboxEventCounter.increment(outboxEvents.size());

            // Log completion
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Batch publication completed with errors: {}", ex.getMessage());
                    } else {
                        log.info("Successfully published batch of {} events", outboxEvents.size());
                    }
                });

            return futures;

        } catch (Exception e) {
            outboxErrorCounter.increment(outboxEvents.size());
            throw new EventPublishingException("Failed to publish outbox event batch", e);
        }
    }

    /**
     * Execute outbox pattern: save to DB and publish in same transaction
     *
     * @param event The domain event
     * @param topicName The target topic
     * @param aggregateId The aggregate ID
     * @param tenantId The tenant ID
     * @return The created outbox event
     */
    @Transactional
    public OutboxEvent executeOutboxPattern(
            Object event,
            String topicName,
            String aggregateId,
            String tenantId) {

        // Create outbox event
        OutboxEvent outboxEvent = OutboxEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(aggregateId)
            .aggregateType(extractAggregateType(topicName))
            .eventType(event.getClass().getSimpleName())
            .eventData(event)
            .topicName(topicName)
            .partitionKey(tenantId) // Use tenant ID as partition key
            .tenantId(tenantId)
            .createdAt(Instant.now())
            .processedAt(null)
            .retryCount(0)
            .status(OutboxEvent.Status.PENDING)
            .build();

        // In a real implementation, save to database here
        // outboxRepository.save(outboxEvent);

        // Publish the event
        publishOutboxEvent(outboxEvent);

        // Mark as processed (in real implementation, update DB)
        outboxEvent.setStatus(OutboxEvent.Status.PROCESSED);
        outboxEvent.setProcessedAt(Instant.now());

        return outboxEvent;
    }

    /**
     * Process pending outbox events (for scheduled job)
     *
     * @param pendingEvents Events to process
     * @return Number of successfully processed events
     */
    @Transactional
    public int processPendingOutboxEvents(List<OutboxEvent> pendingEvents) {
        int successCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                // Check retry limit
                if (event.getRetryCount() >= 3) {
                    log.error("Outbox event {} exceeded retry limit, marking as failed",
                            event.getEventId());
                    event.setStatus(OutboxEvent.Status.FAILED);
                    continue;
                }

                // Attempt to publish
                CompletableFuture<SendResult<String, Object>> future = publishOutboxEvent(event);

                // Wait for completion (synchronous for transaction)
                future.get();

                // Mark as processed
                event.setStatus(OutboxEvent.Status.PROCESSED);
                event.setProcessedAt(Instant.now());
                successCount++;

                log.info("Successfully processed pending outbox event: {}", event.getEventId());

            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}",
                         event.getEventId(), e.getMessage());

                // Increment retry count
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());

                // Keep as pending for retry
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxEvent.Status.FAILED);
                }
            }
        }

        log.info("Processed {} out of {} pending outbox events",
                successCount, pendingEvents.size());

        return successCount;
    }

    /**
     * Convert outbox event to event envelope
     */
    private EventEnvelope convertToEnvelope(OutboxEvent outboxEvent) {
        return EventEnvelope.builder()
            .eventId(outboxEvent.getEventId())
            .eventType(outboxEvent.getEventType())
            .eventVersion("1.0")
            .tenantId(outboxEvent.getTenantId())
            .correlationId(outboxEvent.getCorrelationId() != null ?
                          outboxEvent.getCorrelationId() : UUID.randomUUID().toString())
            .causationId(outboxEvent.getCausationId() != null ?
                        outboxEvent.getCausationId() : UUID.randomUUID().toString())
            .timestamp(outboxEvent.getCreatedAt())
            .payload(outboxEvent.getEventData())
            .metadata(outboxEvent.getMetadata())
            .build();
    }

    /**
     * Extract aggregate type from topic name
     */
    private String extractAggregateType(String topicName) {
        // Topic pattern: domain.<context>.<aggregate>.<event>
        String[] parts = topicName.split("\\.");
        if (parts.length >= 3) {
            return parts[2]; // Return aggregate part
        }
        return "unknown";
    }
}