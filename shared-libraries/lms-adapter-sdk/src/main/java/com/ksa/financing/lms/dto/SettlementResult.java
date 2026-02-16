package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result of an early settlement operation with Ibra.
 */
@Data
@Builder
public class SettlementResult {

    private LoanAccountId loanAccountId;
    private String settlementReference;
    private LocalDateTime settlementDateTime;

    // Settlement Amounts
    private BigDecimal originalOutstanding;
    private BigDecimal ibraAmount;        // Profit waiver
    private BigDecimal finalSettlementAmount;
    private BigDecimal charityAmount;   // Any late fees

    // Payment Details
    private String paymentTransactionId;
    private String paymentMethod;
    private String paymentReference;

    // Status
    private SettlementStatus status;
    private LoanStatus newLoanStatus;
    private boolean success;
    private String errorMessage;

    // Islamic Finance Compliance
    private boolean shariaCompliant;
    private String shariaApprovalRef;

    // Audit
    private String settlementReason;
    private String approvedBy;

    public enum SettlementStatus {
        INITIATED,
        APPROVED,
        PAYMENT_PENDING,
        COMPLETED,
        REJECTED,
        CANCELLED
    }

    public static SettlementResult success(LoanAccountId loanAccountId,
                                          String settlementRef,
                                          BigDecimal settlementAmount) {
        return SettlementResult.builder()
            .loanAccountId(loanAccountId)
            .settlementReference(settlementRef)
            .finalSettlementAmount(settlementAmount)
            .settlementDateTime(LocalDateTime.now())
            .status(SettlementStatus.COMPLETED)
            .newLoanStatus(LoanStatus.CLOSED_PREPAID)
            .success(true)
            .shariaCompliant(true)
            .build();
    }

    public static SettlementResult failed(LoanAccountId loanAccountId,
                                         String errorMessage) {
        return SettlementResult.builder()
            .loanAccountId(loanAccountId)
            .status(SettlementStatus.REJECTED)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}