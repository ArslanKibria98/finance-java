package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletMovementRepository extends JpaRepository<WalletMovementJpaEntity, UUID> {

    List<WalletMovementJpaEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    Optional<WalletMovementJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM WalletMovementJpaEntity m "
            + "WHERE m.tenantId = :tenantId AND m.walletId = :walletId "
            + "AND m.movementType = 'DEBIT' AND m.purpose IN :purposes "
            + "AND m.createdAt >= :since")
    BigDecimal sumSpendSince(@Param("tenantId") UUID tenantId,
                             @Param("walletId") UUID walletId,
                             @Param("purposes") List<String> purposes,
                             @Param("since") OffsetDateTime since);
}
