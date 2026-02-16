package com.ksa.financing.lms.dto;

import lombok.Value;
import java.util.UUID;

/**
 * Value object representing a loan account identifier.
 */
@Value
public class LoanAccountId {
    String value;

    public static LoanAccountId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan account ID cannot be null or empty");
        }
        return new LoanAccountId(value);
    }

    public static LoanAccountId generate() {
        return new LoanAccountId("LOAN-" + UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}