package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfFundsOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfFundsRepository extends JpaRepository<SourceOfFundsOptionJpaEntity, UUID> {
    Optional<SourceOfFundsOptionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<SourceOfFundsOptionJpaEntity> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);
    List<SourceOfFundsOptionJpaEntity> findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
