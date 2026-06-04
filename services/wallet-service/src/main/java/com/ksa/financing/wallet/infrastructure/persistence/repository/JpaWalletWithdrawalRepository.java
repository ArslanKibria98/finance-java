package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletWithdrawalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletWithdrawalRepository extends JpaRepository<WalletWithdrawalJpaEntity, UUID>, JpaSpecificationExecutor<WalletWithdrawalJpaEntity> {

    Optional<WalletWithdrawalJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletWithdrawalJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<WalletWithdrawalJpaEntity> findBySourceWalletIdOrderByInitiatedAtDesc(UUID sourceWalletId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WalletWithdrawalJpaEntity w "
            + "WHERE w.tenantId = :tenantId AND w.sourceWalletId = :walletId "
            + "AND w.status NOT IN :excludedStatuses AND w.initiatedAt >= :since")
    BigDecimal sumWithdrawnSince(@Param("tenantId") UUID tenantId,
                                 @Param("walletId") UUID walletId,
                                 @Param("excludedStatuses") List<String> excludedStatuses,
                                 @Param("since") OffsetDateTime since);
}
