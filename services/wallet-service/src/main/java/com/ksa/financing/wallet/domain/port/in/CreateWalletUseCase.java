package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.Wallet;
import java.util.UUID;

public interface CreateWalletUseCase {
    Wallet create(CreateWalletCommand command);

    record CreateWalletCommand(
        UUID tenantId,
        UUID customerId,
        String currency
    ) {}
}
