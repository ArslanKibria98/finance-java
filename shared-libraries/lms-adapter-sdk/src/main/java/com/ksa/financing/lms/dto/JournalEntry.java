package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a journal entry in the general ledger.
 */
@Data
@Builder
public class JournalEntry {

    private String journalId;
    private LocalDate transactionDate;
    private LocalDateTime createdDateTime;

    // Entry Details
    private String description;
    private String referenceNumber;
    private String referenceType;

    // Journal Lines
    private List<JournalLine> lines;

    // Status
    private JournalStatus status;
    private String approvedBy;
    private LocalDateTime approvalDateTime;

    // Audit
    private String createdBy;
    private String branchCode;
    private String reversalOf;
    private boolean isReversal;

    @Data
    @Builder
    public static class JournalLine {
        private String lineId;
        private GlAccount account;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String description;
        private String costCenter;
        private String analyticsCode;
    }

    public enum JournalStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        POSTED,
        REVERSED,
        REJECTED
    }

    /**
     * Validates that the journal entry is balanced.
     */
    public boolean isBalanced() {
        BigDecimal totalDebits = lines.stream()
            .map(line -> line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = lines.stream()
            .map(line -> line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalDebits.compareTo(totalCredits) == 0;
    }

    public BigDecimal getTotalDebits() {
        return lines.stream()
            .map(line -> line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredits() {
        return lines.stream()
            .map(line -> line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}