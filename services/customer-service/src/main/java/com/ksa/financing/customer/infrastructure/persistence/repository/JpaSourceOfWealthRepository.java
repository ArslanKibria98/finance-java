package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfWealthOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfWealthRepository extends JpaRepository<SourceOfWealthOptionJpaEntity, UUID> {
    Optional<SourceOfWealthOptionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<SourceOfWealthOptionJpaEntity> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);
    List<SourceOfWealthOptionJpaEntity> findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
