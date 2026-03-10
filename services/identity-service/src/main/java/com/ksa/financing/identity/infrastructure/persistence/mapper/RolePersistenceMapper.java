package com.ksa.financing.identity.infrastructure.persistence.mapper;

import com.ksa.financing.identity.domain.model.Role;
import com.ksa.financing.identity.infrastructure.persistence.entity.RoleJpaEntity;

import java.time.ZoneOffset;

public final class RolePersistenceMapper {
    private RolePersistenceMapper() {}

    public static Role toDomain(RoleJpaEntity entity) {
        var role = new Role();
        role.setId(entity.getId());
        role.setTenantId(entity.getTenantId());
        role.setRoleCode(entity.getRoleCode());
        role.setRoleName(entity.getRoleName());
        role.setRoleNameAr(entity.getRoleNameAr());
        role.setDescription(entity.getDescription());
        role.setKeycloakRoleId(entity.getKeycloakRoleId());
        role.setKeycloakRealm(entity.getKeycloakRealm());
        role.setParentRoleId(entity.getParentRoleId());
        role.setActive(entity.isActive());
        role.setSystem(entity.isSystem());
        role.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null);
        role.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant() : null);
        role.setVersion(entity.getVersion());
        return role;
    }

    public static RoleJpaEntity toEntity(Role role) {
        var entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setTenantId(role.getTenantId());
        entity.setRoleCode(role.getRoleCode());
        entity.setRoleName(role.getRoleName());
        entity.setRoleNameAr(role.getRoleNameAr());
        entity.setDescription(role.getDescription());
        entity.setKeycloakRoleId(role.getKeycloakRoleId());
        entity.setKeycloakRealm(role.getKeycloakRealm());
        entity.setParentRoleId(role.getParentRoleId());
        entity.setActive(role.isActive());
        entity.setSystem(role.isSystem());
        entity.setCreatedAt(role.getCreatedAt() != null ? role.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        entity.setUpdatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        entity.setVersion(role.getVersion());
        return entity;
    }
}
