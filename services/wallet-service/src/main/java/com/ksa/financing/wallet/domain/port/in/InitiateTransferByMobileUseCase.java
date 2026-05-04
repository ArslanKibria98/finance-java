package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletTransfer;

import java.math.BigDecimal;
import java.util.UUID;

public interface InitiateTransferByMobileUseCase {

    WalletTransfer initiateByMobile(InitiateByMobileCommand command);

    record InitiateByMobileCommand(
            UUID senderKeycloakUserId,
            String receiverMobile,
            BigDecimal amount,
            String currency,
            String purposeNote,
            String idempotencyKey,
            String initiatorIp,
            String initiatorDeviceId
    ) {}
}
