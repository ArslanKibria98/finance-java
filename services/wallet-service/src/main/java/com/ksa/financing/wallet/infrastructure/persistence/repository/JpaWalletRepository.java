package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWalletRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletJpaEntity> findByCustomerIdAndTenantId(UUID customerId, UUID tenantId);

    Optional<WalletJpaEntity> findByWalletNumberAndTenantId(String walletNumber, UUID tenantId);

    Optional<WalletJpaEntity> findByWalletNumber(String walletNumber);

    Optional<WalletJpaEntity> findByIbanAndTenantId(String iban, UUID tenantId);

    Optional<WalletJpaEntity> findByAccountNumberAndTenantId(String accountNumber, UUID tenantId);

    boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);

    /** Next value from the shared sequence used to mint virtual account numbers. */
    @Query(value = "SELECT nextval('wallet_account_number_seq')", nativeQuery = true)
    long nextAccountNumberSequence();
}
