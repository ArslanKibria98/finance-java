package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletLimitBoundsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletLimitBoundsRepository extends JpaRepository<WalletLimitBoundsJpaEntity, UUID> {
    Optional<WalletLimitBoundsJpaEntity> findByTenantId(UUID tenantId);
}
