package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletLimitChangeRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaWalletLimitChangeRequestRepository
        extends JpaRepository<WalletLimitChangeRequestJpaEntity, UUID> {

    List<WalletLimitChangeRequestJpaEntity> findByTenantIdAndStatusOrderByRequestedAtDesc(UUID tenantId, String status);

    List<WalletLimitChangeRequestJpaEntity> findByTenantIdOrderByRequestedAtDesc(UUID tenantId);

    List<WalletLimitChangeRequestJpaEntity> findByWalletIdOrderByRequestedAtDesc(UUID walletId);

    boolean existsByWalletIdAndStatus(UUID walletId, String status);
}
