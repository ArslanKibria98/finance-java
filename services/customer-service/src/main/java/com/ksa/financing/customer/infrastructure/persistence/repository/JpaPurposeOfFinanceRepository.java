package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.PurposeOfFinanceOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPurposeOfFinanceRepository extends JpaRepository<PurposeOfFinanceOptionJpaEntity, UUID> {
    Optional<PurposeOfFinanceOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<PurposeOfFinanceOptionJpaEntity> findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    List<PurposeOfFinanceOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
