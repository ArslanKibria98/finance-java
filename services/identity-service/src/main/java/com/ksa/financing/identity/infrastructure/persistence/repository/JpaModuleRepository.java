package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.ModuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaModuleRepository extends JpaRepository<ModuleJpaEntity, UUID> {
    List<ModuleJpaEntity> findByTenantIdOrderByDisplayOrder(UUID tenantId);
    List<ModuleJpaEntity> findByTenantIdAndCatalogVisibleTrueOrderByDisplayOrder(UUID tenantId);
    Optional<ModuleJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<ModuleJpaEntity> findByTenantIdAndModuleCode(UUID tenantId, String moduleCode);
}
