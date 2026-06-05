package com.ksa.financing.wallet.domain.port.out;

import java.math.BigDecimal;

/**
 * Output port for Scotiabank Real-Time Payments (RTP / INTERAC e-Transfer for Business),
 * invoked through the middleware-third-party gateway.
 *
 * The platform moves money via a single shared Scotia corporate account (the debtor for
 * outbound payments); each user's virtual account number rides along as the remittance
 * reference. In DEV the middleware runs in mock mode and returns a SUCCESS result.
 */
public interface ScotiaRtpPort {

    /**
     * Submit an outbound real-time payment (options-inquiry then commit).
     * Returns the Scotia payment outcome — never throws for a business reject; inspect {@code success}.
     */
    RtpResult sendPayment(RtpPaymentRequest request);

    record RtpPaymentRequest(
            BigDecimal amount,
            String currency,
            String debtorName,
            String debtorAccount,
            String creditorName,
            String creditorAccount,
            String creditorEmail,
            String messageIdentification,
            String idempotencyKey
    ) {}

    record RtpResult(
            boolean success,
            String paymentId,
            String clearingReference,
            String status,
            String errorCode,
            String errorMessage
    ) {}
}
