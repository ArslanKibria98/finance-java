package com.ksa.financing.collections.domain.model;

/**
 * Strategy for early settlement discount calculation.
 * 1 = Invoice Based (current logic)
 * 2 = Principle Based (new logic)
 */
public enum EarlySettlementStrategy {
    INVOICE_BASED(1),
    PRINCIPLE_BASED(2);

    private final int code;

    EarlySettlementStrategy(int code) { this.code = code; }

    public int code() { return code; }

    public static EarlySettlementStrategy fromCode(int code) {
        for (var s : values()) if (s.code == code) return s;
        return INVOICE_BASED; // Default
    }
}
