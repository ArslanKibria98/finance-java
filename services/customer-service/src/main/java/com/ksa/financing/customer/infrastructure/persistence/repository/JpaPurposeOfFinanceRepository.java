package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.PurposeOfFinanceOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPurposeOfFinanceRepository extends JpaRepository<PurposeOfFinanceOptionJpaEntity, UUID> {
    Optional<PurposeOfFinanceOptionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<PurposeOfFinanceOptionJpaEntity> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);
    List<PurposeOfFinanceOptionJpaEntity> findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
