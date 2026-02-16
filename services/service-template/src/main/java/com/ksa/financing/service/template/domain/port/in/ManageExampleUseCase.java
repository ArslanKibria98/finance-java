package com.ksa.financing.service.template.domain.port.in;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleAggregateId;

/**
 * Input Port: Use case interface for managing example aggregates.
 * This is implemented by the application layer and called by adapters.
 *
 * Following Hexagonal Architecture:
 * - This interface belongs to the domain
 * - Implementation is in the application layer
 * - Called by driving adapters (REST, Temporal, etc.)
 */
public interface ManageExampleUseCase {

    /**
     * Command: Create a new example aggregate.
     */
    ExampleAggregate createExample(CreateExampleCommand command);

    /**
     * Command: Activate an example aggregate.
     */
    void activateExample(ActivateExampleCommand command);

    /**
     * Command: Add entity to an example aggregate.
     */
    void addEntity(AddEntityCommand command);

    /**
     * Command: Complete an example aggregate.
     */
    void completeExample(CompleteExampleCommand command);

    /**
     * Query: Get an example aggregate by ID.
     */
    ExampleAggregate getExample(GetExampleQuery query);

    // Command objects (following CQRS pattern)
    record CreateExampleCommand(
            TenantId tenantId,
            String name,
            String description,
            UserId createdBy
    ) {}

    record ActivateExampleCommand(
            TenantId tenantId,
            ExampleAggregateId aggregateId,
            UserId activatedBy
    ) {}

    record AddEntityCommand(
            TenantId tenantId,
            ExampleAggregateId aggregateId,
            String entityName,
            String entityValue,
            UserId addedBy
    ) {}

    record CompleteExampleCommand(
            TenantId tenantId,
            ExampleAggregateId aggregateId,
            UserId completedBy
    ) {}

    record GetExampleQuery(
            TenantId tenantId,
            ExampleAggregateId aggregateId
    ) {}
}