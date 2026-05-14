package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletWithdrawalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletWithdrawalRepository extends JpaRepository<WalletWithdrawalJpaEntity, UUID>, JpaSpecificationExecutor<WalletWithdrawalJpaEntity> {

    Optional<WalletWithdrawalJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletWithdrawalJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<WalletWithdrawalJpaEntity> findBySourceWalletIdOrderByInitiatedAtDesc(UUID sourceWalletId);
}
