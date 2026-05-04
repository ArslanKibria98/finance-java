package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfWealthOptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfWealthRepository extends JpaRepository<SourceOfWealthOptionJpaEntity, UUID>,
        JpaSpecificationExecutor<SourceOfWealthOptionJpaEntity> {
    Optional<SourceOfWealthOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Page<SourceOfWealthOptionJpaEntity> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    Page<SourceOfWealthOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
