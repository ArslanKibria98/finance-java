package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {
    Optional<RoleJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<RoleJpaEntity> findByTenantIdAndRoleCode(UUID tenantId, String roleCode);
    List<RoleJpaEntity> findAllByTenantIdOrderByRoleCodeAsc(UUID tenantId);
    boolean existsByTenantIdAndRoleCode(UUID tenantId, String roleCode);
    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
