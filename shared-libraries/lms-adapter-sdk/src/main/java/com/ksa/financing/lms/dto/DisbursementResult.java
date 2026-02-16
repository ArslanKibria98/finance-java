package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result of a loan disbursement operation.
 */
@Data
@Builder
public class DisbursementResult {

    private LoanAccountId loanAccountId;
    private String transactionId;
    private BigDecimal disbursedAmount;
    private LocalDateTime disbursementDateTime;

    // Transaction Details
    private String disbursementReference;
    private String paymentMethod;
    private String beneficiaryAccount;

    // Status
    private DisbursementStatus status;
    private boolean success;
    private String errorMessage;

    // Balance Information
    private BigDecimal totalDisbursed;
    private BigDecimal remainingAmount;

    // Islamic Finance Specific
    private String commodityTransferRef;
    private boolean shariaCompliant;

    // For idempotency
    private boolean duplicate;
    private String originalTransactionId;

    public enum DisbursementStatus {
        INITIATED,
        PROCESSING,
        COMPLETED,
        FAILED,
        REVERSED
    }

    public static DisbursementResult success(LoanAccountId loanAccountId,
                                            String transactionId,
                                            BigDecimal amount) {
        return DisbursementResult.builder()
            .loanAccountId(loanAccountId)
            .transactionId(transactionId)
            .disbursedAmount(amount)
            .disbursementDateTime(LocalDateTime.now())
            .status(DisbursementStatus.COMPLETED)
            .success(true)
            .build();
    }

    public static DisbursementResult failed(LoanAccountId loanAccountId,
                                           String errorMessage) {
        return DisbursementResult.builder()
            .loanAccountId(loanAccountId)
            .status(DisbursementStatus.FAILED)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}