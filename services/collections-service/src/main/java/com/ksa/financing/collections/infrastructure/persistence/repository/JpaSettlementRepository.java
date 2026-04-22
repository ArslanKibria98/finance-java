package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.SettlementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JpaSettlementRepository extends JpaRepository<SettlementJpaEntity, UUID> {
    Optional<SettlementJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<SettlementJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}
