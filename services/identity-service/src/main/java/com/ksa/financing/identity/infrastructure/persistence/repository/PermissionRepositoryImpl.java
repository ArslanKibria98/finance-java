package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.domain.model.Permission;
import com.ksa.financing.identity.domain.port.out.PermissionRepository;
import com.ksa.financing.identity.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final JpaPermissionRepository jpaPermissionRepository;
    private final JpaRolePermissionRepository jpaRolePermissionRepository;

    @Override
    public Permission save(Permission permission) {
        var entity = PermissionPersistenceMapper.toEntity(permission);
        var saved = jpaPermissionRepository.save(entity);
        return PermissionPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Permission> findById(UUID tenantId, UUID permissionId) {
        return jpaPermissionRepository.findByTenantIdAndId(tenantId, permissionId)
                .map(PermissionPersistenceMapper::toDomain);
    }

    @Override
    public List<Permission> findAllByTenant(UUID tenantId) {
        return jpaPermissionRepository.findAllByTenantIdOrderByPermissionCodeAsc(tenantId).stream()
                .map(PermissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Permission> findByRoleId(UUID tenantId, UUID roleId) {
        return jpaPermissionRepository.findByRoleId(tenantId, roleId).stream()
                .map(PermissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String permissionCode) {
        return jpaPermissionRepository.existsByTenantIdAndPermissionCode(tenantId, permissionCode);
    }

    @Override
    public void assignToRole(UUID tenantId, UUID roleId, UUID permissionId) {
        if (!jpaRolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            var mapping = new RolePermissionJpaEntity();
            mapping.setTenantId(tenantId);
            mapping.setRoleId(roleId);
            mapping.setPermissionId(permissionId);
            jpaRolePermissionRepository.save(mapping);
        }
    }

    @Override
    public void removeFromRole(UUID tenantId, UUID roleId, UUID permissionId) {
        jpaRolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public void removeAllFromRole(UUID roleId) {
        jpaRolePermissionRepository.deleteAllByRoleId(roleId);
    }
}
