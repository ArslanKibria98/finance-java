package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Result of a loan rescheduling operation in Fineract.
 * Returned after POST /rescheduleloans + approve command.
 */
@Data
@Builder
public class RescheduleResult {

    private LoanAccountId loanAccountId;
    private String requestId;               // Our internal rescheduling_request.id

    // Fineract references
    private String fineractRescheduleId;    // Fineract reschedule request ID
    private String rescheduleReference;     // Human-readable reference

    // New schedule details
    private LocalDate newFirstInstallmentDate;
    private Integer newNumberOfRepayments;
    private BigDecimal newInstallmentAmount;
    private LocalDate newMaturityDate;

    // GL entry references (RESTRUCTURING only)
    private String writeOffJournalEntryId;
    private String profitWaiverJournalEntryId;

    // Status
    private boolean success;
    private String errorMessage;
    private RescheduleStatus status;

    // Audit
    private LocalDateTime processedAt;
    private boolean duplicate;
    private String originalRescheduleId;

    public enum RescheduleStatus {
        SYNCED,          // Successfully synced to Fineract
        PENDING,
        FAILED,
        PARTIALLY_SYNCED // Schedule synced but GL failed (will retry)
    }

    public static RescheduleResult success(LoanAccountId loanAccountId,
                                           String requestId,
                                           String fineractRescheduleId) {
        return RescheduleResult.builder()
                .loanAccountId(loanAccountId)
                .requestId(requestId)
                .fineractRescheduleId(fineractRescheduleId)
                .rescheduleReference("RESCH-" + fineractRescheduleId)
                .status(RescheduleStatus.SYNCED)
                .success(true)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static RescheduleResult failed(LoanAccountId loanAccountId,
                                          String requestId,
                                          String errorMessage) {
        return RescheduleResult.builder()
                .loanAccountId(loanAccountId)
                .requestId(requestId)
                .status(RescheduleStatus.FAILED)
                .success(false)
                .errorMessage(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
