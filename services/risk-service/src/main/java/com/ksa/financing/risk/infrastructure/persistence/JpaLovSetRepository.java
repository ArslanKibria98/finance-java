package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.LovSetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLovSetRepository extends JpaRepository<LovSetJpaEntity, UUID> {
    Optional<LovSetJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<LovSetJpaEntity> findAllByTenantId(UUID tenantId);
    List<LovSetJpaEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
