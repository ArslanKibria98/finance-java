package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.LovEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLovEntryRepository extends JpaRepository<LovEntryJpaEntity, UUID> {
    Optional<LovEntryJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<LovEntryJpaEntity> findAllByTenantIdAndLovSetId(UUID tenantId, UUID lovSetId);
    List<LovEntryJpaEntity> findAllByTenantIdAndLovSetIdAndActiveTrue(UUID tenantId, UUID lovSetId);
    Optional<LovEntryJpaEntity> findByTenantIdAndLovSetIdAndFactorCode(UUID tenantId, UUID lovSetId, String factorCode);
    boolean existsByTenantIdAndLovSetIdAndFactorCode(UUID tenantId, UUID lovSetId, String factorCode);
}
