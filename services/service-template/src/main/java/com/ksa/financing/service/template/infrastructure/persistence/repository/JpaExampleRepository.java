package com.ksa.financing.service.template.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ksa.financing.service.template.infrastructure.persistence.entity.ExampleAggregateJpaEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ExampleAggregateJpaEntity.
 */
@Repository
public interface JpaExampleRepository extends JpaRepository<ExampleAggregateJpaEntity, Long> {

    /**
     * Find by aggregate ID and tenant ID (not deleted).
     */
    @Query("SELECT e FROM ExampleAggregateJpaEntity e " +
           "WHERE e.aggregateId = :aggregateId " +
           "AND e.tenantId = :tenantId " +
           "AND e.deleted = false")
    Optional<ExampleAggregateJpaEntity> findByAggregateIdAndTenantId(
            @Param("aggregateId") String aggregateId,
            @Param("tenantId") String tenantId
    );

    /**
     * Find all by tenant ID (not deleted).
     */
    @Query("SELECT e FROM ExampleAggregateJpaEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.deleted = false " +
           "ORDER BY e.createdAt DESC")
    List<ExampleAggregateJpaEntity> findAllByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find by status and tenant ID (not deleted).
     */
    @Query("SELECT e FROM ExampleAggregateJpaEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.status = :status " +
           "AND e.deleted = false " +
           "ORDER BY e.createdAt DESC")
    List<ExampleAggregateJpaEntity> findByStatusAndTenantId(
            @Param("status") String status,
            @Param("tenantId") String tenantId
    );

    /**
     * Check existence by aggregate ID and tenant ID (not deleted).
     */
    @Query("SELECT COUNT(e) > 0 FROM ExampleAggregateJpaEntity e " +
           "WHERE e.aggregateId = :aggregateId " +
           "AND e.tenantId = :tenantId " +
           "AND e.deleted = false")
    boolean existsByAggregateIdAndTenantId(
            @Param("aggregateId") String aggregateId,
            @Param("tenantId") String tenantId
    );
}