package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reschedule Intent - Domain request for loan rescheduling.
 * Supports all 4 rescheduling types per Blueprint 17.
 *
 * Fineract sync: POST /rescheduleloans (submit) + POST /rescheduleloans/{id}?command=approve
 * GL entries (RESTRUCTURING only): POST /journalentries via createJournalEntry()
 */
@Data
@Builder
public class RescheduleIntent {

    private String loanAccountId;
    private String requestId;          // Our internal rescheduling_request.id

    // Type of rescheduling (Blueprint 17: 4 types)
    private RescheduleType rescheduleType;

    // Common fields
    private LocalDate rescheduleFromDate;  // Which installment onwards
    private String rescheduleReasonId;     // Fineract reason code
    private String rescheduleReasonComment;

    // TENURE_EXTENSION
    private Integer extensionMonths;       // 1-12 months (max per Blueprint 17)
    private Integer newNumberOfRepayments; // Original + extension

    // SKIP_PAYMENT
    private Integer graceOnPrincipalPayment;  // Number of installments to skip
    private Integer graceOnInterestPayment;

    // PAYMENT_HOLIDAY / TENURE_EXTENSION
    private BigDecimal newInstallmentAmount;

    // RESTRUCTURING — write-off GL entries
    private BigDecimal writeOffAmount;        // Principal write-off
    private BigDecimal profitWaiverAmount;    // Unearned profit waiver
    private String writeOffGlAccountCode;     // PROVISION_FOR_BAD_DEBTS
    private String loanReceivableGlCode;      // LOAN_RECEIVABLE_PRINCIPAL
    private String unearnedProfitGlCode;      // UNEARNED_PROFIT
    private String profitReceivableGlCode;    // LOAN_RECEIVABLE_PROFIT

    // Idempotency
    private String idempotencyKey;

    // Audit
    private String submittedBy;

    public enum RescheduleType {
        SKIP_PAYMENT,
        TENURE_EXTENSION,
        PAYMENT_HOLIDAY,
        RESTRUCTURING
    }
}
