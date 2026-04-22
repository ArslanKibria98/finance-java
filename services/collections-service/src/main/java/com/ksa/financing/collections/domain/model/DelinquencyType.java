package com.ksa.financing.collections.domain.model;

/**
 * Six lifecycle stages of delinquency management, mirroring the LMS admin UI.
 * Integer codes are stable wire values shared with the front-end.
 */
public enum DelinquencyType {
    EARLY_SETTLEMENT(1),
    DUE_LOAN(2),
    LATE_PAYMENT(3),
    WRITE_OFFS(4),
    NON_PERFORMING_LOAN(5),
    BROKEN_PROMISES(6);

    private final int code;

    DelinquencyType(int code) { this.code = code; }

    public int code() { return code; }

    public static DelinquencyType fromCode(int code) {
        for (var t : values()) if (t.code == code) return t;
        throw new IllegalArgumentException("Unknown delinquencyType code: " + code);
    }
}
