package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestDevJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRequestDevRepository
        extends JpaRepository<ClientRequestDevJpaEntity, UUID>,
                JpaSpecificationExecutor<ClientRequestDevJpaEntity> {

    Optional<ClientRequestDevJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ClientRequestDevJpaEntity> findByRequestIdAndTenantId(String requestId, UUID tenantId);

    Optional<ClientRequestDevJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);
}
