package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.PurposeOfFinanceOptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaPurposeOfFinanceRepository extends JpaRepository<PurposeOfFinanceOptionJpaEntity, UUID>,
        JpaSpecificationExecutor<PurposeOfFinanceOptionJpaEntity> {
    Optional<PurposeOfFinanceOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Page<PurposeOfFinanceOptionJpaEntity> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    Page<PurposeOfFinanceOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
