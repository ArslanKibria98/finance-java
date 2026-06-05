package com.ksa.financing.customer.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface WalletPort {

    Optional<String> getIbanByCustomerId(UUID tenantId, UUID customerId);

    /** Fetch wallet identifiers (iban + virtual account number + wallet number) in one call. */
    Optional<WalletInfo> getWalletInfoByCustomerId(UUID tenantId, UUID customerId);

    record WalletInfo(String iban, String accountNumber, String walletNumber) {}
}
