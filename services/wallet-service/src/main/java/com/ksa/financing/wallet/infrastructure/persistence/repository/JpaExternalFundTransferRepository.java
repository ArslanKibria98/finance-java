package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.ExternalFundTransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaExternalFundTransferRepository
        extends JpaRepository<ExternalFundTransferJpaEntity, UUID>,
        JpaSpecificationExecutor<ExternalFundTransferJpaEntity> {

    Optional<ExternalFundTransferJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ExternalFundTransferJpaEntity> findByMovementId(UUID movementId);

    Optional<ExternalFundTransferJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}
