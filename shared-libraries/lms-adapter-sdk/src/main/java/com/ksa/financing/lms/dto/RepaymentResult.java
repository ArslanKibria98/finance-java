package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result of a loan repayment operation.
 */
@Data
@Builder
public class RepaymentResult {

    private LoanAccountId loanAccountId;
    private String transactionId;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentDateTime;

    // Payment Allocation
    private BigDecimal principalPaid;
    private BigDecimal profitPaid;
    private BigDecimal penaltyPaid;
    private BigDecimal charityAmount; // Islamic finance late fees

    // Balance Information
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingProfit;
    private BigDecimal totalOutstanding;

    // Transaction Details
    private String paymentReference;
    private String receiptNumber;
    private String paymentMethod;

    // Status
    private boolean success;
    private String errorMessage;
    private PaymentStatus status;

    // Next Payment Info
    private LocalDateTime nextDueDate;
    private BigDecimal nextInstallmentAmount;

    // For duplicate detection
    private boolean duplicate;
    private String originalTransactionId;

    public enum PaymentStatus {
        POSTED,
        PENDING,
        FAILED,
        REVERSED
    }

    public static RepaymentResult success(LoanAccountId loanAccountId,
                                         String transactionId,
                                         BigDecimal amount) {
        return RepaymentResult.builder()
            .loanAccountId(loanAccountId)
            .transactionId(transactionId)
            .paymentAmount(amount)
            .paymentDateTime(LocalDateTime.now())
            .status(PaymentStatus.POSTED)
            .success(true)
            .build();
    }

    public static RepaymentResult failed(LoanAccountId loanAccountId,
                                        String errorMessage) {
        return RepaymentResult.builder()
            .loanAccountId(loanAccountId)
            .status(PaymentStatus.FAILED)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}