package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Settlement Intent - Domain request for early loan settlement with Ibra.
 */
@Data
@Builder
public class SettlementIntent {

    private String loanAccountId;
    private LocalDate settlementDate;

    // Settlement Amounts (calculated by domain)
    private BigDecimal outstandingPrincipal;
    private BigDecimal remainingProfit;
    private BigDecimal ibraAmount;         // Profit waiver amount
    private BigDecimal settlementAmount;  // Final amount to pay
    private BigDecimal charityAmount;     // Any late fees go to charity

    // Ibra Calculation Details
    private String ibraCalculationMethod;  // UNEARNED_PROFIT, PERCENTAGE_BASED
    private BigDecimal ibraPercentage;    // If percentage-based
    private String ibraApprovalReference;

    // Payment Details
    private String paymentMethod;
    private String paymentReference;
    private String transactionId;

    // Islamic Finance Compliance
    private boolean shariaboardApproved;
    private String shariaApprovalReference;
    private String ibraJustification;    // Reason for profit waiver

    // For idempotency
    private String idempotencyKey;

    // Audit
    private String requestedBy;
    private String approvedBy;
    private String settlementReason;  // CUSTOMER_REQUEST, REFINANCING, etc.
}