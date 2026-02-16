package com.ksa.financing.service.template.domain.model;

import lombok.Getter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import com.ksa.financing.domain.base.AggregateRoot;
import com.ksa.financing.domain.base.DomainEvent;
import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Example Aggregate Root following DDD patterns.
 * This demonstrates:
 * - Aggregate boundaries
 * - Domain invariants
 * - Event sourcing
 * - Business logic encapsulation
 */
@Slf4j
@Getter
public class ExampleAggregate extends AggregateRoot<ExampleAggregateId> {

    private final ExampleAggregateId id;
    private final TenantId tenantId;
    private String name;
    private String description;
    private ExampleStatus status;
    private final UserId createdBy;
    private final LocalDateTime createdAt;
    private UserId lastModifiedBy;
    private LocalDateTime lastModifiedAt;

    // Collection of child entities (part of aggregate boundary)
    private final List<ExampleEntity> entities;

    // Private constructor enforces creation through factory method
    private ExampleAggregate(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.lastModifiedBy = builder.lastModifiedBy;
        this.lastModifiedAt = builder.lastModifiedAt;
        this.entities = new ArrayList<>(builder.entities);
    }

    /**
     * Factory method to create a new aggregate.
     * Ensures invariants are satisfied and emits creation event.
     */
    public static ExampleAggregate create(
            TenantId tenantId,
            String name,
            String description,
            UserId createdBy) {

        // Validate invariants
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        var aggregate = ExampleAggregate.builder()
                .id(ExampleAggregateId.generate())
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .status(ExampleStatus.DRAFT)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .lastModifiedBy(createdBy)
                .lastModifiedAt(LocalDateTime.now())
                .entities(new ArrayList<>())
                .build();

        // Raise domain event
        aggregate.registerEvent(new ExampleAggregateCreated(
                aggregate.id,
                tenantId,
                name,
                createdBy
        ));

        log.debug("Created ExampleAggregate: {}", aggregate.id);
        return aggregate;
    }

    /**
     * Business operation: Activate the aggregate.
     * Demonstrates state transitions and invariant checking.
     */
    public void activate(UserId activatedBy) {
        // Check preconditions
        if (status != ExampleStatus.DRAFT) {
            throw new IllegalStateException(
                "Can only activate from DRAFT status, current: " + status
            );
        }

        // Perform state transition
        this.status = ExampleStatus.ACTIVE;
        this.lastModifiedBy = activatedBy;
        this.lastModifiedAt = LocalDateTime.now();

        // Raise event
        registerEvent(new ExampleAggregateActivated(
                id, tenantId, activatedBy
        ));

        log.debug("Activated ExampleAggregate: {}", id);
    }

    /**
     * Business operation: Add an entity to the aggregate.
     * Child entities are part of the aggregate boundary.
     */
    public void addEntity(String entityName, String entityValue, UserId addedBy) {
        // Validate
        if (status != ExampleStatus.ACTIVE) {
            throw new IllegalStateException(
                "Can only add entities when ACTIVE"
            );
        }

        // Check aggregate invariant
        if (entities.size() >= 10) {
            throw new IllegalStateException(
                "Cannot have more than 10 entities"
            );
        }

        // Create and add entity
        var entity = new ExampleEntity(
                UUID.randomUUID().toString(),
                entityName,
                entityValue
        );
        entities.add(entity);

        this.lastModifiedBy = addedBy;
        this.lastModifiedAt = LocalDateTime.now();

        // Raise event
        registerEvent(new ExampleEntityAdded(
                id, tenantId, entity.getId(), entityName, addedBy
        ));
    }

    /**
     * Business operation: Complete the aggregate.
     */
    public void complete(UserId completedBy) {
        if (status != ExampleStatus.ACTIVE) {
            throw new IllegalStateException(
                "Can only complete from ACTIVE status"
            );
        }

        if (entities.isEmpty()) {
            throw new IllegalStateException(
                "Cannot complete without any entities"
            );
        }

        this.status = ExampleStatus.COMPLETED;
        this.lastModifiedBy = completedBy;
        this.lastModifiedAt = LocalDateTime.now();

        registerEvent(new ExampleAggregateCompleted(
                id, tenantId, completedBy
        ));
    }

    @Builder
    private static class Builder {
        private ExampleAggregateId id;
        private TenantId tenantId;
        private String name;
        private String description;
        private ExampleStatus status;
        private UserId createdBy;
        private LocalDateTime createdAt;
        private UserId lastModifiedBy;
        private LocalDateTime lastModifiedAt;
        private List<ExampleEntity> entities;
    }

    // Domain Events
    public record ExampleAggregateCreated(
            ExampleAggregateId aggregateId,
            TenantId tenantId,
            String name,
            UserId createdBy
    ) implements DomainEvent {}

    public record ExampleAggregateActivated(
            ExampleAggregateId aggregateId,
            TenantId tenantId,
            UserId activatedBy
    ) implements DomainEvent {}

    public record ExampleEntityAdded(
            ExampleAggregateId aggregateId,
            TenantId tenantId,
            String entityId,
            String entityName,
            UserId addedBy
    ) implements DomainEvent {}

    public record ExampleAggregateCompleted(
            ExampleAggregateId aggregateId,
            TenantId tenantId,
            UserId completedBy
    ) implements DomainEvent {}
}