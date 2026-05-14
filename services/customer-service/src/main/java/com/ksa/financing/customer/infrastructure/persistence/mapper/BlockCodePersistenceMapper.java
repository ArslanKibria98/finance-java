package com.ksa.financing.customer.infrastructure.persistence.mapper;

import com.ksa.financing.customer.domain.model.BlockCode;
import com.ksa.financing.customer.domain.model.BlockCodeCategory;
import com.ksa.financing.customer.domain.model.BlockCodeType;
import com.ksa.financing.customer.domain.model.CustomerBlock;
import com.ksa.financing.customer.infrastructure.persistence.entity.BlockCodeJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerBlockJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class BlockCodePersistenceMapper {

    public static BlockCodeJpaEntity toEntity(BlockCode domain) {
        if (domain == null) return null;
        BlockCodeJpaEntity entity = new BlockCodeJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setDescription(domain.getDescription());
        entity.setType(domain.getType());
        entity.setCategory(domain.getCategory());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        return entity;
    }

    public static BlockCode toDomain(BlockCodeJpaEntity entity) {
        if (entity == null) return null;
        return BlockCode.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .type(entity.getType())
                .category(entity.getCategory())
                .active(entity.isActive())
                .createdAt(toInstant(entity.getCreatedAt()))
                .updatedAt(toInstant(entity.getUpdatedAt()))
                .build();
    }

    public static CustomerBlockJpaEntity toEntity(CustomerBlock domain, BlockCodeJpaEntity blockCodeEntity) {
        if (domain == null) return null;
        CustomerBlockJpaEntity entity = new CustomerBlockJpaEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setBlockCode(blockCodeEntity);
        entity.setAssignedBy(domain.getAssignedBy());
        entity.setReason(domain.getReason());
        entity.setAssignedAt(toOffsetDateTime(domain.getAssignedAt()));
        entity.setExpiresAt(toOffsetDateTime(domain.getExpiresAt()));
        entity.setActive(domain.isActive());
        return entity;
    }

    public static CustomerBlock toDomain(CustomerBlockJpaEntity entity) {
        if (entity == null) return null;
        return CustomerBlock.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .blockCodeId(entity.getBlockCode().getId())
                .blockCode(entity.getBlockCode().getCode())
                .blockDescription(entity.getBlockCode().getDescription())
                .blockType(entity.getBlockCode().getType())
                .assignedBy(entity.getAssignedBy())
                .reason(entity.getReason())
                .assignedAt(toInstant(entity.getAssignedAt()))
                .expiresAt(toInstant(entity.getExpiresAt()))
                .active(entity.isActive())
                .build();
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private static Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }
}
