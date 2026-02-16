package com.ksa.islamic.reporting.projector;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the last processed event for each projector.
 * Used for idempotency and resumption after failures.
 */
@Entity
@Table(name = "projection_checkpoints",
        indexes = {
                @Index(name = "idx_checkpoint_projector", columnList = "projector_name"),
                @Index(name = "idx_checkpoint_event", columnList = "event_id"),
                @Index(name = "idx_checkpoint_timestamp", columnList = "event_timestamp"),
                @Index(name = "idx_checkpoint_composite", columnList = "projector_name, event_id", unique = true)
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectionCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "projector_name", nullable = false, length = 255)
    private String projectorName;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "partition", nullable = false)
    private int partition;

    @Column(name = "offset", nullable = false)
    private long offset;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Check if this checkpoint is older than the specified duration.
     */
    public boolean isOlderThan(java.time.Duration duration) {
        return processedAt.plus(duration).isBefore(Instant.now());
    }

    /**
     * Calculate the processing lag (time between event creation and processing).
     */
    public java.time.Duration getProcessingLag() {
        return java.time.Duration.between(eventTimestamp, processedAt);
    }
}