package com.ksa.financing.wallet.domain.port.out;

import java.math.BigDecimal;

/**
 * Outbound port for submitting payments to external bank rails (SAMA SARIE / IPS).
 * <p>
 * The default implementation is a mock simulator. In production, this is wired to
 * a real SARIE adapter behind a circuit breaker.
 */
public interface BankRailsPort {

    /**
     * Submit a payment to bank rails. Returns a receipt with the network reference
     * and final settlement status. Implementations should be synchronous from the
     * caller's perspective — async settlement (T+0/T+1) is handled inside the adapter.
     */
    SubmissionReceipt submit(SubmissionRequest request);

    record SubmissionRequest(
            String withdrawalNumber,
            String idempotencyKey,
            String beneficiaryName,
            String destinationIban,
            String destinationBankCode,
            BigDecimal amount,
            String currency,
            String purposeCode,
            String narrative
    ) {}

    record SubmissionReceipt(
            boolean accepted,
            String bankReference,
            String sarieReference,
            String settlementStatus,   // ACCEPTED | SETTLED | REJECTED
            String rejectionCode,
            String rejectionMessage
    ) {}
}
