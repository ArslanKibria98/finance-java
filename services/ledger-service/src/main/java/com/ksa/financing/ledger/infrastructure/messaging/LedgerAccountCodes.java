package com.ksa.financing.ledger.infrastructure.messaging;

/**
 * Standard COA codes used by auto-journalization from loan lifecycle events.
 * These accounts are seeded via V3__test_data.sql and assumed to exist per tenant.
 */
public final class LedgerAccountCodes {

    private LedgerAccountCodes() {}

    public static final String BANK_ACCOUNT = "1010";
    public static final String LOANS_RECEIVABLE = "1200";
    public static final String PROFIT_INCOME = "4010";
    public static final String PROVISION_FOR_BAD_DEBTS = "3100";

    public static final String REF_TYPE_LOAN = "LOAN";
    public static final String REF_TYPE_PAYMENT = "PAYMENT";
    public static final String REF_TYPE_SETTLEMENT = "SETTLEMENT";
    public static final String REF_TYPE_OVERDUE = "OVERDUE";

    public static final String TXN_TYPE_DISBURSEMENT = "DISBURSEMENT";
    public static final String TXN_TYPE_REPAYMENT = "REPAYMENT";
    public static final String TXN_TYPE_SETTLEMENT = "SETTLEMENT";
    public static final String TXN_TYPE_PROVISION = "PROVISION";
}
