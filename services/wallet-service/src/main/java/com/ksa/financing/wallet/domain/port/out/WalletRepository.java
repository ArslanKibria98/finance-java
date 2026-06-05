package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.Wallet;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(UUID id);
    Optional<Wallet> findByCustomerId(UUID tenantId, UUID customerId);
    Optional<Wallet> findByWalletNumber(String walletNumber);
    Optional<Wallet> findByIban(UUID tenantId, String iban);
    Optional<Wallet> findByAccountNumber(UUID tenantId, String accountNumber);
    boolean existsByCustomerId(UUID tenantId, UUID customerId);

    /** Atomically mint the next virtual account-number sequence value. */
    long nextAccountNumberSequence();
}
