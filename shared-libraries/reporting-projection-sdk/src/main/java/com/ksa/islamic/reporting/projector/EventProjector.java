package com.ksa.islamic.reporting.projector;

import com.ksa.financing.domain.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for all event projectors in the CQRS read model.
 * Handles domain events and projects them into denormalized read models.
 *
 * @param <T> The type of domain event this projector handles
 */
@Slf4j
public abstract class EventProjector<T extends BaseProjectableEvent> {

    protected final ProjectionCheckpointRepository checkpointRepository;
    protected final MeterRegistry meterRegistry;
    protected final Tracer tracer;

    // Metrics
    private final Counter eventsProcessed;
    private final Counter eventsFailure;
    private final Timer projectionTimer;

    protected EventProjector(
            ProjectionCheckpointRepository checkpointRepository,
            MeterRegistry meterRegistry,
            Tracer tracer) {
        this.checkpointRepository = checkpointRepository;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;

        // Initialize metrics
        String projectorName = this.getClass().getSimpleName();
        this.eventsProcessed = Counter.builder("projection.events.processed")
                .tag("projector", projectorName)
                .description("Number of events successfully processed")
                .register(meterRegistry);

        this.eventsFailure = Counter.builder("projection.events.failed")
                .tag("projector", projectorName)
                .description("Number of events that failed processing")
                .register(meterRegistry);

        this.projectionTimer = Timer.builder("projection.processing.time")
                .tag("projector", projectorName)
                .description("Time taken to process an event")
                .register(meterRegistry);
    }

    /**
     * Main entry point for Kafka event consumption.
     * Subclasses should override this with specific topic configuration.
     */
    @KafkaListener(topics = "#{__listener.topics}", groupId = "#{__listener.groupId}")
    @Transactional
    public void handleEvent(
            @Payload T event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            Acknowledgment acknowledgment) {

        Span span = tracer.spanBuilder("projection.handle")
                .setAttribute("event.type", event.getEventType())
                .setAttribute("aggregate.id", event.getAggregateId())
                .startSpan();

        try {
            log.debug("Processing event: {} from topic: {}, partition: {}, offset: {}",
                    event.getEventType(), topic, partition, offset);

            Instant start = Instant.now();

            // Check if event was already processed (idempotency)
            if (isEventAlreadyProcessed(event)) {
                log.debug("Event {} already processed, skipping", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Apply projection strategy
            ProjectionStrategy strategy = determineStrategy(event);

            switch (strategy) {
                case INCREMENTAL:
                    projectIncremental(event);
                    break;
                case REBUILD:
                    projectRebuild(event);
                    break;
                case SKIP:
                    log.debug("Skipping event: {}", event.getEventId());
                    break;
            }

            // Update checkpoint
            updateCheckpoint(event, topic, partition, offset, timestamp);

            // Record metrics
            Duration processingTime = Duration.between(start, Instant.now());
            projectionTimer.record(processingTime);
            eventsProcessed.increment();

            // Acknowledge message
            acknowledgment.acknowledge();

            log.debug("Successfully processed event: {} in {}ms",
                    event.getEventId(), processingTime.toMillis());

        } catch (Exception e) {
            span.recordException(e);
            eventsFailure.increment();
            log.error("Failed to process event: {}", event.getEventId(), e);
            handleProjectionError(event, e);
            // Don't acknowledge - let Kafka retry
            throw new ProjectionException("Failed to project event", e);
        } finally {
            span.end();
        }
    }

    /**
     * Project the event incrementally (update existing read model).
     */
    protected abstract void projectIncremental(T event);

    /**
     * Rebuild the entire read model from scratch.
     * Used for complex aggregations or when incremental updates are not feasible.
     */
    protected abstract void projectRebuild(T event);

    /**
     * Determine the projection strategy for this event.
     */
    protected abstract ProjectionStrategy determineStrategy(T event);

    /**
     * Get the topics this projector listens to.
     */
    public abstract String[] getTopics();

    /**
     * Get the consumer group ID for this projector.
     */
    public abstract String getGroupId();

    /**
     * Check if an event has already been processed (for idempotency).
     */
    protected boolean isEventAlreadyProcessed(T event) {
        return checkpointRepository
                .findByProjectorNameAndEventId(getProjectorName(), event.getEventId().toString())
                .isPresent();
    }

    /**
     * Update the projection checkpoint after successful processing.
     */
    protected void updateCheckpoint(T event, String topic, int partition, long offset, long timestamp) {
        ProjectionCheckpoint checkpoint = ProjectionCheckpoint.builder()
                .projectorName(getProjectorName())
                .eventId(event.getEventId().toString())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .processedAt(Instant.now())
                .eventTimestamp(Instant.ofEpochMilli(timestamp))
                .build();

        checkpointRepository.save(checkpoint);
    }

    /**
     * Handle projection errors with retry logic and dead letter queue.
     */
    protected void handleProjectionError(T event, Exception error) {
        // Log to error tracking system
        log.error("Projection error for event: {} of type: {}",
                event.getEventId(), event.getEventType(), error);

        // Could implement retry logic or send to DLQ here
        // For now, we'll let Kafka's built-in retry mechanism handle it
    }

    /**
     * Get the name of this projector (used for checkpointing).
     */
    protected String getProjectorName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Rebuild projection from a specific point in time.
     * Useful for recovering from corruption or bugs.
     */
    public CompletableFuture<Void> rebuildFromTimestamp(Instant fromTimestamp) {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting rebuild of {} from timestamp: {}", getProjectorName(), fromTimestamp);
            // Implementation would query event store and replay events
            // This is a placeholder for the actual implementation
        });
    }

    /**
     * Get the current lag (difference between latest event and last processed).
     */
    public long getCurrentLag() {
        // This would query Kafka for the latest offset and compare with checkpoint
        return 0L; // Placeholder
    }
}