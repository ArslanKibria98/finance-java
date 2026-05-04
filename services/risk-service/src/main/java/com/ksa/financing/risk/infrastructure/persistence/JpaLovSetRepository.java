package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.LovSetJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLovSetRepository extends JpaRepository<LovSetJpaEntity, UUID>,
        JpaSpecificationExecutor<LovSetJpaEntity> {
    Optional<LovSetJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<LovSetJpaEntity> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<LovSetJpaEntity> findAllByTenantIdAndActiveTrue(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
