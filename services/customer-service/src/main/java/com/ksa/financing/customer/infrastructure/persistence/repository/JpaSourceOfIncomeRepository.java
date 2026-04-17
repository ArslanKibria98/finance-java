package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfIncomeOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfIncomeRepository extends JpaRepository<SourceOfIncomeOptionJpaEntity, UUID> {
    Optional<SourceOfIncomeOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<SourceOfIncomeOptionJpaEntity> findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    List<SourceOfIncomeOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
