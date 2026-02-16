package com.ksa.financing.service.template.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleEntity;
import com.ksa.financing.service.template.application.dto.ExampleAggregateDto;
import com.ksa.financing.service.template.domain.service.ExampleDomainService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between domain models and DTOs.
 * Keeps domain models isolated from external representation.
 */
@Mapper(componentModel = "spring")
public abstract class ExampleAggregateMapper {

    @Autowired
    protected ExampleDomainService domainService;

    /**
     * Map domain aggregate to DTO.
     */
    @Mapping(source = "id.value", target = "id")
    @Mapping(source = "tenantId.value", target = "tenantId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdBy.value", target = "createdBy")
    @Mapping(source = "lastModifiedBy.value", target = "lastModifiedBy")
    @Mapping(source = "entities", target = "entities", qualifiedByName = "mapEntities")
    @Mapping(source = "aggregate", target = "completionScore", qualifiedByName = "calculateScore")
    public abstract ExampleAggregateDto toDto(ExampleAggregate aggregate);

    /**
     * Map list of domain aggregates to DTOs.
     */
    public abstract List<ExampleAggregateDto> toDtos(List<ExampleAggregate> aggregates);

    /**
     * Custom mapping for entities.
     */
    @Named("mapEntities")
    protected List<ExampleAggregateDto.ExampleEntityDto> mapEntities(List<ExampleEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapEntity)
                .collect(Collectors.toList());
    }

    /**
     * Map single entity.
     */
    protected ExampleAggregateDto.ExampleEntityDto mapEntity(ExampleEntity entity) {
        return ExampleAggregateDto.ExampleEntityDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .value(entity.getValue())
                .build();
    }

    /**
     * Calculate completion score using domain service.
     */
    @Named("calculateScore")
    protected Double calculateScore(ExampleAggregate aggregate) {
        if (aggregate == null) {
            return 0.0;
        }
        return domainService.calculateCompletionScore(aggregate);
    }
}