package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaRoleRepository extends JpaRepository<RoleJpaEntity, UUID>, JpaSpecificationExecutor<RoleJpaEntity> {
    Optional<RoleJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<RoleJpaEntity> findByTenantIdAndRoleCode(UUID tenantId, String roleCode);
    boolean existsByTenantIdAndRoleCode(UUID tenantId, String roleCode);
    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
