package com.ksa.financing.identity.infrastructure.persistence.mapper;

import com.ksa.financing.identity.domain.model.Permission;
import com.ksa.financing.identity.infrastructure.persistence.entity.PermissionJpaEntity;

import java.time.ZoneOffset;

public final class PermissionPersistenceMapper {
    private PermissionPersistenceMapper() {}

    public static Permission toDomain(PermissionJpaEntity entity) {
        var perm = new Permission();
        perm.setId(entity.getId());
        perm.setTenantId(entity.getTenantId());
        perm.setPermissionCode(entity.getPermissionCode());
        perm.setPermissionName(entity.getPermissionName());
        perm.setDescription(entity.getDescription());
        perm.setResourceType(entity.getResourceType());
        perm.setModuleId(entity.getModuleId());
        perm.setAction(entity.getAction());
        perm.setActive(entity.isActive());
        perm.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null);
        perm.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant() : null);
        return perm;
    }

    public static PermissionJpaEntity toEntity(Permission perm) {
        var entity = new PermissionJpaEntity();
        entity.setId(perm.getId());
        entity.setTenantId(perm.getTenantId());
        entity.setPermissionCode(perm.getPermissionCode());
        entity.setPermissionName(perm.getPermissionName());
        entity.setDescription(perm.getDescription());
        entity.setResourceType(perm.getResourceType());
        entity.setModuleId(perm.getModuleId());
        entity.setAction(perm.getAction());
        entity.setActive(perm.isActive());
        entity.setCreatedAt(perm.getCreatedAt() != null ? perm.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        entity.setUpdatedAt(perm.getUpdatedAt() != null ? perm.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        return entity;
    }
}
