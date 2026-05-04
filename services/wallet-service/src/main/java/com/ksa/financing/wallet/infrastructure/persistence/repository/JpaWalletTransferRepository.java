package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletTransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletTransferRepository extends JpaRepository<WalletTransferJpaEntity, UUID> {

    Optional<WalletTransferJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletTransferJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<WalletTransferJpaEntity> findBySourceWalletIdOrderByInitiatedAtDesc(UUID sourceWalletId);

    List<WalletTransferJpaEntity> findByDestinationWalletIdOrderByInitiatedAtDesc(UUID destinationWalletId);
}
