package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.FineractProxyIdempotencyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFineractProxyIdempotencyRepository
        extends JpaRepository<FineractProxyIdempotencyJpaEntity, FineractProxyIdempotencyJpaEntity.PK> {

    Optional<FineractProxyIdempotencyJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    @Modifying
    @Query("DELETE FROM FineractProxyIdempotencyJpaEntity e WHERE e.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
