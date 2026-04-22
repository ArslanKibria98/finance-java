package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for idempotency_keys table.
 */
public interface JpaIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyJpaEntity, UUID> {

    Optional<IdempotencyKeyJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<IdempotencyKeyJpaEntity> findAllByExpiresAtBefore(LocalDateTime now);
}
