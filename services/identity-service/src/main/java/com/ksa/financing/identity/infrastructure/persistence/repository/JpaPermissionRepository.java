package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.PermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPermissionRepository extends JpaRepository<PermissionJpaEntity, UUID>, JpaSpecificationExecutor<PermissionJpaEntity> {
    Optional<PermissionJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndPermissionCode(UUID tenantId, String permissionCode);

    @Query("SELECT p FROM PermissionJpaEntity p JOIN RolePermissionJpaEntity rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.tenantId = :tenantId ORDER BY p.permissionCode ASC")
    List<PermissionJpaEntity> findByRoleId(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);
}
