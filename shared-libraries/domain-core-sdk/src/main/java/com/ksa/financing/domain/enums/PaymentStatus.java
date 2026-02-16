package com.ksa.financing.domain.enums;

/**
 * Payment transaction status.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED,
    CANCELLED
}
