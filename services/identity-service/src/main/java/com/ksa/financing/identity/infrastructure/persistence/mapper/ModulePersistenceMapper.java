package com.ksa.financing.identity.infrastructure.persistence.mapper;

import com.ksa.financing.identity.domain.model.Module;
import com.ksa.financing.identity.infrastructure.persistence.entity.ModuleJpaEntity;

import java.time.ZoneOffset;

public final class ModulePersistenceMapper {
    private ModulePersistenceMapper() {}

    public static Module toDomain(ModuleJpaEntity entity) {
        var module = new Module();
        module.setId(entity.getId());
        module.setTenantId(entity.getTenantId());
        module.setModuleCode(entity.getModuleCode());
        module.setModuleName(entity.getModuleName());
        module.setDescription(entity.getDescription());
        module.setActive(entity.isActive());
        module.setDisplayOrder(entity.getDisplayOrder());
        module.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null);
        module.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant() : null);
        return module;
    }

    public static ModuleJpaEntity toEntity(Module module) {
        var entity = new ModuleJpaEntity();
        entity.setId(module.getId());
        entity.setTenantId(module.getTenantId());
        entity.setModuleCode(module.getModuleCode());
        entity.setModuleName(module.getModuleName());
        entity.setDescription(module.getDescription());
        entity.setActive(module.isActive());
        entity.setDisplayOrder(module.getDisplayOrder());
        entity.setCreatedAt(module.getCreatedAt() != null ? module.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        entity.setUpdatedAt(module.getUpdatedAt() != null ? module.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        return entity;
    }
}
