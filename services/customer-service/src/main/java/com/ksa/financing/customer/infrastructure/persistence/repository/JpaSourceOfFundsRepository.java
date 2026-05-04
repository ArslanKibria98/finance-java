package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfFundsOptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfFundsRepository extends JpaRepository<SourceOfFundsOptionJpaEntity, UUID>,
        JpaSpecificationExecutor<SourceOfFundsOptionJpaEntity> {
    Optional<SourceOfFundsOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Page<SourceOfFundsOptionJpaEntity> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    Page<SourceOfFundsOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
