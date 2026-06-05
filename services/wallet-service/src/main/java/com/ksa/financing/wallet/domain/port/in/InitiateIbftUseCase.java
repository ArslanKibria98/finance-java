package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.IbftTransaction;

import java.math.BigDecimal;
import java.util.UUID;

/** Initiate an IBFT transfer (wallet → external Canadian bank account via Scotia EFT, HOLD-based). */
public interface InitiateIbftUseCase {

    IbftTransaction initiate(InitiateIbftCommand command);

    record InitiateIbftCommand(
            UUID tenantId,
            UUID customerId,
            UUID walletId,
            UUID beneficiaryId,          // saved beneficiary ... OR the one-time fields below
            String institutionNumber,
            String accountNumber,        // transit derived from its first 5 digits
            String beneficiaryName,
            String bankName,
            BigDecimal amount,
            String currency,
            String purposeNote,
            String idempotencyKey,
            UUID initiatorUserId,
            String initiatorIp,
            String initiatorDeviceId
    ) {}
}
