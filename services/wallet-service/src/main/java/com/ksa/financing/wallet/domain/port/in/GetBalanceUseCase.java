package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.Wallet;
import java.util.UUID;

public interface GetBalanceUseCase {
    Wallet getByWalletId(UUID walletId);
    Wallet getByCustomerId(UUID tenantId, UUID customerId);
}
