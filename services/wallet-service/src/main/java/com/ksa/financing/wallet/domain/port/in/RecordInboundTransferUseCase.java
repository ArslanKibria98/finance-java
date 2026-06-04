package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound external fund transfer: external money has arrived in the platform's Scotia
 * corporate account destined for a user's virtual account number — credit their wallet.
 *
 * Triggered by the internal endpoint (future: Scotia inbound webhook / notification).
 */
public interface RecordInboundTransferUseCase {

    ExternalFundTransfer record(RecordInboundTransferCommand command);

    record RecordInboundTransferCommand(
            UUID tenantId,
            String accountNumber,       // either the user's virtual account ...
            UUID customerId,            // ... or customerId (one is required)
            BigDecimal amount,
            String currency,
            String senderName,
            String senderAccount,
            String reference,
            String idempotencyKey
    ) {}
}
