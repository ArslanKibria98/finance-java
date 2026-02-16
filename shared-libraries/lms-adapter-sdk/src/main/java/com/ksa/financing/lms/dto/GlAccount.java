package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a General Ledger account.
 */
@Data
@Builder
public class GlAccount {

    private String accountCode;
    private String accountName;
    private String accountType;
    private String parentCode;

    // KSA Chart of Accounts specific
    private String coaCategory;    // As per SAMA requirements
    private String analyticsCode;  // For reporting
    private String costCenter;

    // Account Properties
    private boolean isActive;
    private boolean isControlAccount;
    private boolean allowManualEntry;

    // Islamic Finance Specific
    private boolean isIslamicAccount;
    private String shariaCategory;  // PROFIT, CHARITY, COMMODITY, etc.

    public static GlAccount of(String accountCode, String accountName) {
        return GlAccount.builder()
            .accountCode(accountCode)
            .accountName(accountName)
            .isActive(true)
            .build();
    }

    // Common KSA GL Accounts
    public static final GlAccount CASH = of("1001", "Cash and Cash Equivalents");
    public static final GlAccount LOANS_RECEIVABLE = of("1201", "Loans Receivable");
    public static final GlAccount PROFIT_RECEIVABLE = of("1202", "Profit Receivable");
    public static final GlAccount CUSTOMER_LIABILITY = of("2001", "Customer Deposits");
    public static final GlAccount DEFERRED_PROFIT = of("2101", "Deferred Profit Income");
    public static final GlAccount PROFIT_INCOME = of("4001", "Profit Income");
    public static final GlAccount FEE_INCOME = of("4101", "Fee Income");
    public static final GlAccount CHARITY_PAYABLE = of("2201", "Charity Payable");
}