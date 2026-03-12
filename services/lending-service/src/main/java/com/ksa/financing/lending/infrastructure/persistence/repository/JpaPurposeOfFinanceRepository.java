package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.PurposeOfFinanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPurposeOfFinanceRepository extends JpaRepository<PurposeOfFinanceJpaEntity, UUID> {

    List<PurposeOfFinanceJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);

    List<PurposeOfFinanceJpaEntity> findByTenantIdOrderBySortOrder(UUID tenantId);

    Optional<PurposeOfFinanceJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<PurposeOfFinanceJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);

    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
