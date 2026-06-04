package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound external fund transfer: move money from a user wallet to an external
 * Canadian bank account via Scotia RTP.
 */
public interface InitiateExternalTransferUseCase {

    ExternalFundTransfer initiate(InitiateExternalTransferCommand command);

    record InitiateExternalTransferCommand(
            UUID tenantId,
            UUID sourceWalletId,        // either walletId ...
            UUID customerId,            // ... or customerId (one is required)
            String counterpartyName,
            String counterpartyAccount,
            String counterpartyEmail,
            String counterpartyBankCode,
            BigDecimal amount,
            String currency,
            String purposeNote,
            String idempotencyKey,
            UUID initiatorUserId,
            String initiatorIp,
            String initiatorDeviceId
    ) {}
}
