package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfFundsOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfFundsRepository extends JpaRepository<SourceOfFundsOptionJpaEntity, UUID> {
    Optional<SourceOfFundsOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<SourceOfFundsOptionJpaEntity> findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    List<SourceOfFundsOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
