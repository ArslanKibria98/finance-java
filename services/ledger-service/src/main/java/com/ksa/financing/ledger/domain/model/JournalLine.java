package com.ksa.financing.ledger.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a single line in a journal entry.
 * Invariant: exactly one of debitAmount or creditAmount must be non-zero.
 * Pure domain class — zero framework imports.
 */
public record JournalLine(
        AccountId accountId,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String description,
        int lineNumber
) {

    public JournalLine {
        Objects.requireNonNull(accountId, "AccountId cannot be null");
        Objects.requireNonNull(debitAmount, "Debit amount cannot be null");
        Objects.requireNonNull(creditAmount, "Credit amount cannot be null");

        boolean debitPositive = debitAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean creditPositive = creditAmount.compareTo(BigDecimal.ZERO) > 0;

        if (debitPositive && creditPositive) {
            throw new IllegalArgumentException(
                    "Journal line cannot have both debit and credit amounts positive. " +
                    "Account: " + accountId);
        }
        if (!debitPositive && !creditPositive) {
            throw new IllegalArgumentException(
                    "Journal line must have either a debit or credit amount > 0. " +
                    "Account: " + accountId);
        }
        if (debitAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Debit amount cannot be negative");
        }
        if (creditAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit amount cannot be negative");
        }
    }

    public static JournalLine debit(AccountId accountId, BigDecimal amount, String description, int lineNumber) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        return new JournalLine(accountId, amount, BigDecimal.ZERO, description, lineNumber);
    }

    public static JournalLine credit(AccountId accountId, BigDecimal amount, String description, int lineNumber) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        return new JournalLine(accountId, BigDecimal.ZERO, amount, description, lineNumber);
    }

    public boolean isDebit() {
        return debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCredit() {
        return creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal effectiveAmount() {
        return isDebit() ? debitAmount : creditAmount;
    }
}
