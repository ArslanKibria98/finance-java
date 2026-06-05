package com.ksa.financing.service.template.domain.port.out;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleAggregateId;
import com.ksa.financing.service.template.domain.model.ExampleStatus;

import java.util.List;
import java.util.Optional;

/**
 * Output Port: Repository interface for ExampleAggregate persistence.
 * This is implemented by the infrastructure layer.
 *
 * Following Hexagonal Architecture:
 * - This interface belongs to the domain
 * - Implementation is in the infrastructure layer (JPA adapter)
 * - Domain has no knowledge of how data is stored
 */
public interface ExampleRepository {

    /**
     * Save an aggregate (create or update).
     */
    ExampleAggregate save(ExampleAggregate aggregate);

    /**
     * Find an aggregate by ID within a tenant.
     */
    Optional<ExampleAggregate> findById(
            TenantId tenantId,
            ExampleAggregateId aggregateId
    );

    /**
     * Find all aggregates for a tenant.
     */
    List<ExampleAggregate> findAllByTenant(TenantId tenantId);

    /**
     * Find aggregates by status within a tenant.
     */
    List<ExampleAggregate> findByStatus(
            TenantId tenantId,
            ExampleStatus status
    );

    /**
     * Check if an aggregate exists.
     */
    boolean exists(TenantId tenantId, ExampleAggregateId aggregateId);

    /**
     * Delete an aggregate (soft delete recommended).
     */
    void delete(TenantId tenantId, ExampleAggregateId aggregateId);
}