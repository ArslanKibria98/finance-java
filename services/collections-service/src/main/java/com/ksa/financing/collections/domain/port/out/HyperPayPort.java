package com.ksa.financing.collections.domain.port.out;

import java.math.BigDecimal;

/**
 * Output port for HyperPay payment gateway operations.
 * Calls middleware-third-party service which proxies to the actual HyperPay API.
 * Zero framework imports.
 */
public interface HyperPayPort {

    /**
     * Creates a HyperPay checkout session.
     * Returns the checkout ID used to render the payment widget.
     */
    HyperPayCheckoutResult createCheckout(BigDecimal amount, String currency,
                                           String paymentBrand, String idempotencyKey);

    /**
     * Fetches the payment status for a completed checkout.
     * Called after the customer completes payment on the widget.
     */
    HyperPayStatusResult getPaymentStatus(String checkoutId);

    /**
     * Refunds a completed HyperPay payment.
     */
    HyperPayRefundResult refundPayment(String paymentId, BigDecimal amount, String currency);

    // ==================== RESULT RECORDS ====================

    record HyperPayCheckoutResult(
            boolean success,
            String checkoutId,
            String resultCode,
            String resultDescription,
            String rawResponse
    ) {}

    record HyperPayStatusResult(
            boolean success,
            boolean paymentSuccessful,
            String paymentId,
            String resultCode,
            String resultDescription,
            String paymentBrand,
            String amount,
            String currency,
            String merchantTransactionId,
            String rawResponse
    ) {}

    record HyperPayRefundResult(
            boolean success,
            String refundId,
            String resultCode,
            String resultDescription,
            String rawResponse
    ) {}
}
