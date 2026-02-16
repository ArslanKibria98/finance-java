package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Journal Intent - Domain request for creating journal entries in the GL.
 */
@Data
@Builder
public class JournalIntent {

    private String transactionId;
    private LocalDate transactionDate;
    private String description;

    // Journal Entry Type
    private JournalType journalType;

    // Double-entry bookkeeping
    private List<JournalLine> debitLines;
    private List<JournalLine> creditLines;

    // Reference Information
    private String referenceNumber;
    private String referenceType; // LOAN_DISBURSEMENT, PAYMENT, etc.
    private String loanAccountId;

    // For reversals
    private boolean isReversal;
    private String originalJournalId;

    // Audit
    private String createdBy;
    private String approvedBy;
    private String branchCode;

    @Data
    @Builder
    public static class JournalLine {
        private String glAccountCode;
        private String glAccountName;
        private BigDecimal amount;
        private String costCenter;
        private String description;
        private String analyticsCode; // For reporting
    }

    public enum JournalType {
        LOAN_DISBURSEMENT,
        LOAN_REPAYMENT,
        PROFIT_ACCRUAL,
        CHARITY_ALLOCATION,  // Islamic finance late fees
        FEE_INCOME,
        PROVISION_EXPENSE,
        WRITE_OFF,
        REVERSAL
    }

    /**
     * Validates that total debits equal total credits.
     */
    public boolean isBalanced() {
        BigDecimal totalDebits = debitLines.stream()
            .map(JournalLine::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = creditLines.stream()
            .map(JournalLine::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalDebits.compareTo(totalCredits) == 0;
    }
}