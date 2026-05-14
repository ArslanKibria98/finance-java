package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestTestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRequestTestRepository
        extends JpaRepository<ClientRequestTestJpaEntity, UUID>,
                JpaSpecificationExecutor<ClientRequestTestJpaEntity> {

    Optional<ClientRequestTestJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ClientRequestTestJpaEntity> findByRequestIdAndTenantId(String requestId, UUID tenantId);

    Optional<ClientRequestTestJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);
}
