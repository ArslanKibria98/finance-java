package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestProdJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRequestProdRepository
        extends JpaRepository<ClientRequestProdJpaEntity, UUID>,
                JpaSpecificationExecutor<ClientRequestProdJpaEntity> {

    Optional<ClientRequestProdJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ClientRequestProdJpaEntity> findByRequestIdAndTenantId(String requestId, UUID tenantId);

    Optional<ClientRequestProdJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);
}
