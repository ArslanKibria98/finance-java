package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the balance of a GL account.
 */
@Data
@Builder
public class AccountBalance {

    private GlAccount account;
    private BigDecimal debitBalance;
    private BigDecimal creditBalance;
    private BigDecimal netBalance;
    private LocalDate asOfDate;

    // Period Information
    private BigDecimal openingBalance;
    private BigDecimal periodDebits;
    private BigDecimal periodCredits;
    private BigDecimal closingBalance;

    // Account Type Info
    private AccountType accountType;
    private boolean isDebitAccount;

    public enum AccountType {
        ASSET,
        LIABILITY,
        EQUITY,
        REVENUE,
        EXPENSE
    }

    /**
     * Calculate the actual balance based on account type.
     * Debit accounts (Assets, Expenses) have normal debit balance.
     * Credit accounts (Liabilities, Equity, Revenue) have normal credit balance.
     */
    public BigDecimal getActualBalance() {
        if (isDebitAccount) {
            return debitBalance.subtract(creditBalance);
        } else {
            return creditBalance.subtract(debitBalance);
        }
    }
}