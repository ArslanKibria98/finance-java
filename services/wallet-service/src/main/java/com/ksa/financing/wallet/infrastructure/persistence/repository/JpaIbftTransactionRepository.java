package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftTransactionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaIbftTransactionRepository
        extends JpaRepository<IbftTransactionJpaEntity, UUID>,
        JpaSpecificationExecutor<IbftTransactionJpaEntity> {

    Optional<IbftTransactionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<IbftTransactionJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    @Query("SELECT t FROM IbftTransactionJpaEntity t WHERE t.status IN ('SUBMITTED','PROCESSING') "
            + "ORDER BY t.lastInquiredAt ASC NULLS FIRST, t.initiatedAt ASC")
    List<IbftTransactionJpaEntity> findReconcilable(Pageable pageable);
}
