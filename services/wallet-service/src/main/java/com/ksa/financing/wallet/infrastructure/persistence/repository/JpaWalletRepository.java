package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletJpaEntity> findByCustomerIdAndTenantId(UUID customerId, UUID tenantId);

    Optional<WalletJpaEntity> findByWalletNumberAndTenantId(String walletNumber, UUID tenantId);

    Optional<WalletJpaEntity> findByWalletNumber(String walletNumber);

    boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
}
