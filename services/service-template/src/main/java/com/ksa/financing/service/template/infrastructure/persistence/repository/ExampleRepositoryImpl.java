package com.ksa.financing.service.template.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleAggregateId;
import com.ksa.financing.service.template.domain.model.ExampleStatus;
import com.ksa.financing.service.template.domain.port.out.ExampleRepository;
import com.ksa.financing.service.template.infrastructure.persistence.mapper.ExamplePersistenceMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ExampleRepository using JPA.
 * This adapter translates between domain models and JPA entities.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExampleRepositoryImpl implements ExampleRepository {

    private final JpaExampleRepository jpaRepository;
    private final ExamplePersistenceMapper mapper;

    @Override
    @Transactional
    public ExampleAggregate save(ExampleAggregate aggregate) {
        log.debug("Saving aggregate: {}", aggregate.getId());

        // Convert domain model to JPA entity
        var entity = mapper.toEntity(aggregate);

        // Save and flush to get generated IDs
        entity = jpaRepository.saveAndFlush(entity);

        // Convert back to domain model
        var savedAggregate = mapper.toDomain(entity);

        log.debug("Saved aggregate with JPA ID: {}", entity.getId());
        return savedAggregate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExampleAggregate> findById(
            TenantId tenantId,
            ExampleAggregateId aggregateId) {

        log.debug("Finding aggregate: {} for tenant: {}", aggregateId, tenantId);

        return jpaRepository.findByAggregateIdAndTenantId(
                aggregateId.getValue(),
                tenantId.getValue()
        ).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExampleAggregate> findAllByTenant(TenantId tenantId) {
        log.debug("Finding all aggregates for tenant: {}", tenantId);

        return jpaRepository.findAllByTenantId(tenantId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExampleAggregate> findByStatus(
            TenantId tenantId,
            ExampleStatus status) {

        log.debug("Finding aggregates by status: {} for tenant: {}", status, tenantId);

        return jpaRepository.findByStatusAndTenantId(
                status.name(),
                tenantId.getValue()
        ).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(TenantId tenantId, ExampleAggregateId aggregateId) {
        return jpaRepository.existsByAggregateIdAndTenantId(
                aggregateId.getValue(),
                tenantId.getValue()
        );
    }

    @Override
    @Transactional
    public void delete(TenantId tenantId, ExampleAggregateId aggregateId) {
        log.debug("Soft deleting aggregate: {} for tenant: {}", aggregateId, tenantId);

        jpaRepository.findByAggregateIdAndTenantId(
                aggregateId.getValue(),
                tenantId.getValue()
        ).ifPresent(entity -> {
            // Soft delete
            entity.setDeleted(true);
            entity.setDeletedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}