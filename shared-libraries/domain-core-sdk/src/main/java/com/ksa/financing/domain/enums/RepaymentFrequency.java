package com.ksa.financing.domain.enums;

/**
 * Repayment installment frequency.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public enum RepaymentFrequency {
    MONTHLY(1),
    QUARTERLY(3),
    SEMI_ANNUAL(6),
    ANNUAL(12);

    private final int months;

    RepaymentFrequency(int months) {
        this.months = months;
    }

    public int getMonths() {
        return months;
    }
}
