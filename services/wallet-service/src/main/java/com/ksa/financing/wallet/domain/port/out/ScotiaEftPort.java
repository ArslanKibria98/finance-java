package com.ksa.financing.wallet.domain.port.out;

import java.math.BigDecimal;

/**
 * Output port for Scotiabank EFT Payments (batch/clearing rail), via middleware-third-party.
 * Flow: account-validation → create (JWS) → submit → inquire (async settlement).
 */
public interface ScotiaEftPort {

    /** Validate a Canadian bank account (institution + transit + account). */
    AccountValidationResult validateAccount(String institutionNumber, String transit,
                                            String accountNumber, String fullName);

    /** Create an EFT submission (returns submission_id). Idempotent via endToEndId. */
    EftCreateResult createPayment(EftPaymentRequest request);

    /** Submit a previously created submission for processing. */
    EftSubmitResult submit(String submissionId, String idempotencyKey);

    /** Inquire submission status (cron reconciliation). */
    EftInquiryResult inquire(String submissionId);

    record AccountValidationResult(boolean valid, String status, String ref, String raw) {}

    record EftPaymentRequest(
            BigDecimal amount,
            String currency,
            String debtorName,
            String debtorAccount,
            String creditorName,
            String creditorAccount,
            String endToEndId,
            String idempotencyKey
    ) {}

    record EftCreateResult(boolean success, String submissionId, String paymentId,
                           String status, String errorCode, String errorMessage) {}

    record EftSubmitResult(boolean success, String status, String errorCode, String errorMessage) {}

    /** reachable=false means the inquiry call itself failed (keep PROCESSING). */
    record EftInquiryResult(boolean reachable, String submissionStatus, String paymentStatus,
                            boolean settled, boolean rejected, String raw) {}
}
