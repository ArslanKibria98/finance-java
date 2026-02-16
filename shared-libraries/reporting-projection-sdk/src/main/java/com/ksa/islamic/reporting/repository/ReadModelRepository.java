package com.ksa.islamic.reporting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

/**
 * Base interface for all read-only repositories in the CQRS read model.
 * Provides common query methods without modification capabilities.
 *
 * @param <T> The type of the read model entity
 * @param <ID> The type of the entity's identifier
 */
public interface ReadModelRepository<T, ID> {

    /**
     * Find an entity by its ID.
     */
    Optional<T> findById(ID id);

    /**
     * Find all entities.
     */
    List<T> findAll();

    /**
     * Find all entities with pagination.
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Find entities matching a specification.
     */
    List<T> findAll(Specification<T> spec);

    /**
     * Find entities matching a specification with pagination.
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);

    /**
     * Count all entities.
     */
    long count();

    /**
     * Count entities matching a specification.
     */
    long count(Specification<T> spec);

    /**
     * Check if an entity exists by ID.
     */
    boolean existsById(ID id);

    /**
     * Refresh the materialized view (if applicable).
     * This is a no-op for regular tables.
     */
    default void refreshView() {
        // Default implementation does nothing
        // Override in specific repositories for materialized views
    }

    /**
     * Get query performance statistics.
     */
    default QueryStatistics getQueryStatistics() {
        return QueryStatistics.empty();
    }
}