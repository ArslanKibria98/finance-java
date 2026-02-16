package com.ksa.financing.domain.enums;

/**
 * Payment collection methods.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public enum PaymentMethod {
    /** Automatic debit from wallet */
    WALLET_AUTO_DEBIT,

    /** Direct bank account debit */
    BANK_DEBIT,

    /** SADAD bill payment */
    SADAD,

    /** Credit/debit card payment */
    CARD,

    /** Bank transfer */
    BANK_TRANSFER,

    /** Cash deposit */
    CASH,

    /** Apple Pay */
    APPLE_PAY,

    /** Mada (Saudi debit card) */
    MADA
}
