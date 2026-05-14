package com.ksa.financing.risk.infrastructure.persistence.mapper;

import com.ksa.financing.risk.domain.model.BlockCode;
import com.ksa.financing.risk.infrastructure.persistence.entity.BlockCodeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BlockCodePersistenceMapper {

    public BlockCode toDomain(BlockCodeJpaEntity entity) {
        if (entity == null) return null;
        return BlockCode.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .code(entity.getCode())
            .description(entity.getDescription())
            .type(entity.getType())
            .active(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .version(entity.getVersion())
            .build();
    }

    public BlockCodeJpaEntity toJpa(BlockCode domain) {
        if (domain == null) return null;
        BlockCodeJpaEntity entity = new BlockCodeJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setDescription(domain.getDescription());
        entity.setType(domain.getType());
        entity.setActive(domain.isActive());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
