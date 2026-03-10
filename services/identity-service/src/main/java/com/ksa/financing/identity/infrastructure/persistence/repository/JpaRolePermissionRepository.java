package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaRolePermissionRepository extends JpaRepository<RolePermissionJpaEntity, UUID> {
    Optional<RolePermissionJpaEntity> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    void deleteAllByRoleId(UUID roleId);
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
