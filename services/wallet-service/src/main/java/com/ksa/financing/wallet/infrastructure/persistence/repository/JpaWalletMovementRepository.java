package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletMovementRepository extends JpaRepository<WalletMovementJpaEntity, UUID> {

    List<WalletMovementJpaEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    Optional<WalletMovementJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);
}
