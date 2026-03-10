package com.ksa.financing.service.template.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.service.template.domain.model.*;
import com.ksa.financing.service.template.infrastructure.persistence.entity.ExampleAggregateJpaEntity;
import com.ksa.financing.service.template.infrastructure.persistence.entity.ExampleEntityJpaEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between domain models and JPA entities.
 * This keeps the domain layer independent of persistence concerns.
 */
@Component
public class ExamplePersistenceMapper {

    /**
     * Convert domain aggregate to JPA entity.
     */
    public ExampleAggregateJpaEntity toEntity(ExampleAggregate aggregate) {
        var entity = new ExampleAggregateJpaEntity();

        // Map basic fields
        entity.setAggregateId(aggregate.getId().getValue());
        entity.setTenantId(aggregate.getTenantId().getValue());
        entity.setName(aggregate.getName());
        entity.setDescription(aggregate.getDescription());
        entity.setStatus(aggregate.getStatus().name());
        entity.setCreatedBy(aggregate.getCreatedBy().getValue());
        entity.setCreatedAt(aggregate.getCreatedAt());

        if (aggregate.getLastModifiedBy() != null) {
            entity.setLastModifiedBy(aggregate.getLastModifiedBy().getValue());
        }
        entity.setLastModifiedAt(aggregate.getLastModifiedAt());

        // Map child entities
        var childEntities = aggregate.getEntities().stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        // Clear and re-add to maintain relationship
        entity.getEntities().clear();
        childEntities.forEach(entity::addEntity);

        return entity;
    }

    /**
     * Convert JPA entity to domain aggregate.
     */
    public ExampleAggregate toDomain(ExampleAggregateJpaEntity entity) {
        // Map child entities first
        List<ExampleEntity> domainEntities = entity.getEntities().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        // Use builder to create immutable aggregate
        return ExampleAggregate.builder()
                .id(ExampleAggregateId.of(entity.getAggregateId()))
                .tenantId(new TenantId(entity.getTenantId()))
                .name(entity.getName())
                .description(entity.getDescription())
                .status(ExampleStatus.valueOf(entity.getStatus()))
                .createdBy(new UserId(entity.getCreatedBy()))
                .createdAt(entity.getCreatedAt())
                .lastModifiedBy(entity.getLastModifiedBy() != null
                    ? new UserId(entity.getLastModifiedBy())
                    : null)
                .lastModifiedAt(entity.getLastModifiedAt())
                .entities(domainEntities)
                .build();
    }

    /**
     * Convert domain entity to JPA entity.
     */
    private ExampleEntityJpaEntity toEntity(ExampleEntity domainEntity) {
        var entity = new ExampleEntityJpaEntity();
        entity.setEntityId(domainEntity.getId());
        entity.setName(domainEntity.getName());
        entity.setValue(domainEntity.getValue());
        return entity;
    }

    /**
     * Convert JPA entity to domain entity.
     */
    private ExampleEntity toDomain(ExampleEntityJpaEntity entity) {
        return new ExampleEntity(
                entity.getEntityId(),
                entity.getName(),
                entity.getValue()
        );
    }
}