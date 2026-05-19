package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletTransferJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletTransferRepository extends JpaRepository<WalletTransferJpaEntity, UUID>, JpaSpecificationExecutor<WalletTransferJpaEntity> {

    Optional<WalletTransferJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletTransferJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<WalletTransferJpaEntity> findBySourceWalletIdOrderByInitiatedAtDesc(UUID sourceWalletId);

    List<WalletTransferJpaEntity> findByDestinationWalletIdOrderByInitiatedAtDesc(UUID destinationWalletId);

    @Query("SELECT t.destinationWalletId FROM WalletTransferJpaEntity t " +
           "WHERE t.sourceWalletId = :sourceWalletId " +
           "GROUP BY t.destinationWalletId " +
           "ORDER BY MAX(t.initiatedAt) DESC")
    List<UUID> findRecentRecipientWalletIds(@Param("sourceWalletId") UUID sourceWalletId, Pageable pageable);

    Optional<WalletTransferJpaEntity> findFirstBySourceWalletIdAndDestinationWalletIdOrderByInitiatedAtDesc(
            UUID sourceWalletId, UUID destinationWalletId);
}
