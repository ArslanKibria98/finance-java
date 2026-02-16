package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Repayment Intent - Domain request for recording a payment.
 */
@Data
@Builder
public class RepaymentIntent {

    private String loanAccountId;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentDateTime;

    // Payment Source
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private String transactionId;

    // Payment Allocation
    private PaymentType paymentType;
    private BigDecimal principalAmount;
    private BigDecimal profitAmount;
    private BigDecimal penaltyAmount; // Goes to charity in Islamic finance
    private BigDecimal charityAmount;  // Late payment charity

    // For partial payments
    private boolean isPartialPayment;
    private int installmentNumber;

    // Bank/Payment Details
    private String payerAccountNumber;
    private String payerBankCode;
    private String receiptNumber;

    // For idempotency
    private String idempotencyKey;

    // Audit
    private String collectedBy;
    private String collectionChannel;

    public enum PaymentMethod {
        BANK_TRANSFER,
        SADAD,           // Saudi bill payment
        WALLET_DEBIT,
        CASH,
        CHECK,
        MADA_CARD,
        APPLE_PAY,
        AUTO_DEBIT
    }

    public enum PaymentType {
        REGULAR_INSTALLMENT,
        EARLY_SETTLEMENT,
        PARTIAL_PREPAYMENT,
        OVERDUE_PAYMENT,
        ADVANCE_PAYMENT
    }
}