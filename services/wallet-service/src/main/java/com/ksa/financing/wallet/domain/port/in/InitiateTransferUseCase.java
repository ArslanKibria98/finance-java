package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletTransfer;

import java.math.BigDecimal;
import java.util.UUID;

public interface InitiateTransferUseCase {

    WalletTransfer initiate(InitiateTransferCommand command);

    record InitiateTransferCommand(
            UUID tenantId,
            UUID sourceWalletId,
            UUID destinationWalletId,
            String destinationWalletNumber,
            BigDecimal amount,
            String currency,
            String purposeNote,
            String channel,
            String idempotencyKey,
            UUID initiatorUserId,
            String initiatorIp,
            String initiatorDeviceId
    ) {}
}
