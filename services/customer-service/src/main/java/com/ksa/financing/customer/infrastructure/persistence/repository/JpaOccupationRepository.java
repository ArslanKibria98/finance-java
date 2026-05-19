package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.OccupationOptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaOccupationRepository extends JpaRepository<OccupationOptionJpaEntity, UUID>,
        JpaSpecificationExecutor<OccupationOptionJpaEntity> {
    Optional<OccupationOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Page<OccupationOptionJpaEntity> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    Page<OccupationOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
