package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.TopUpTransaction;
import java.math.BigDecimal;
import java.util.UUID;

public interface TopUpUseCase {
    TopUpTransaction topUp(TopUpCommand command);

    record TopUpCommand(
        UUID tenantId,
        UUID walletId,
        BigDecimal amount,
        String method,
        String sourceIban,
        String idempotencyKey
    ) {}
}
