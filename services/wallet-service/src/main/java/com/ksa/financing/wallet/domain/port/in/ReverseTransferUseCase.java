package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletTransfer;

import java.util.UUID;

public interface ReverseTransferUseCase {

    WalletTransfer reverse(ReverseTransferCommand command);

    record ReverseTransferCommand(
            UUID tenantId,
            UUID transferId,
            UUID adminUserId,
            String reason,
            String idempotencyKey
    ) {}
}
