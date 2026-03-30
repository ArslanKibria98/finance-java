package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfIncomeOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfIncomeRepository extends JpaRepository<SourceOfIncomeOptionJpaEntity, UUID> {
    Optional<SourceOfIncomeOptionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<SourceOfIncomeOptionJpaEntity> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);
    List<SourceOfIncomeOptionJpaEntity> findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
