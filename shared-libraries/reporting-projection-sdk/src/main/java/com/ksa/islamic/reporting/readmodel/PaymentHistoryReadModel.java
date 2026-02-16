package com.ksa.islamic.reporting.readmodel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Denormalized read model for payment history.
 * Optimized for payment analytics and collection tracking.
 */
@Entity
@Table(name = "payment_history_read_model",
        indexes = {
                @Index(name = "idx_payment_loan", columnList = "loan_id"),
                @Index(name = "idx_payment_customer", columnList = "customer_id"),
                @Index(name = "idx_payment_date", columnList = "payment_date"),
                @Index(name = "idx_payment_due_date", columnList = "due_date"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_channel", columnList = "payment_channel"),
                @Index(name = "idx_payment_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentHistoryReadModel {

    @Id
    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    // Tenant isolation
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Loan reference
    @Column(name = "loan_id", nullable = false)
    private String loanId;

    @Column(name = "loan_account_number")
    private String loanAccountNumber; // Denormalized

    // Customer reference (denormalized)
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "national_id")
    private String nationalId;

    // Payment details
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "scheduled_amount", precision = 19, scale = 4)
    private BigDecimal scheduledAmount;

    // Payment breakdown
    @Column(name = "principal_portion", precision = 19, scale = 4)
    private BigDecimal principalPortion;

    @Column(name = "interest_portion", precision = 19, scale = 4)
    private BigDecimal interestPortion;

    @Column(name = "late_fee", precision = 19, scale = 4)
    private BigDecimal lateFee;

    @Column(name = "other_charges", precision = 19, scale = 4)
    private BigDecimal otherCharges;

    // Islamic finance specific
    @Column(name = "profit_portion", precision = 19, scale = 4)
    private BigDecimal profitPortion; // For Murabaha/Tawarruq

    @Column(name = "rental_portion", precision = 19, scale = 4)
    private BigDecimal rentalPortion; // For Ijara

    // Payment method
    @Column(name = "payment_channel")
    private String paymentChannel; // BANK_TRANSFER, SADAD, CASH, CHEQUE, AUTO_DEBIT

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "bank_reference")
    private String bankReference;

    // Status
    @Column(name = "status", nullable = false)
    private String status; // PENDING, COMPLETED, BOUNCED, FAILED, REVERSED

    @Column(name = "reconciliation_status")
    private String reconciliationStatus; // PENDING, RECONCILED, MISMATCH

    // Timing
    @Column(name = "days_late")
    private Integer daysLate; // Negative means early payment

    @Column(name = "is_prepayment")
    private Boolean isPrepayment;

    @Column(name = "is_partial_payment")
    private Boolean isPartialPayment;

    // Installment tracking
    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    // Balances after payment
    @Column(name = "outstanding_after", precision = 19, scale = 4)
    private BigDecimal outstandingAfter;

    @Column(name = "overdue_after", precision = 19, scale = 4)
    private BigDecimal overdueAfter;

    // Collection tracking
    @Column(name = "collection_attempt")
    private Integer collectionAttempt;

    @Column(name = "collector_id")
    private String collectorId;

    @Column(name = "collection_notes")
    @Column(columnDefinition = "TEXT")
    private String collectionNotes;

    // Receipt details
    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "receipt_generated")
    private Boolean receiptGenerated;

    // Branch/Channel info
    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "teller_id")
    private String tellerId;

    // Reversal info (if applicable)
    @Column(name = "reversed")
    private Boolean reversed;

    @Column(name = "reversal_date")
    private LocalDate reversalDate;

    @Column(name = "reversal_reason")
    private String reversalReason;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Computed methods

    /**
     * Check if payment was on time.
     */
    @Transient
    public boolean isOnTime() {
        return daysLate != null && daysLate <= 0;
    }

    /**
     * Get payment efficiency (actual vs scheduled).
     */
    @Transient
    public BigDecimal getPaymentEfficiency() {
        if (scheduledAmount == null || scheduledAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return paymentAmount
                .divide(scheduledAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if payment needs reconciliation.
     */
    @Transient
    public boolean needsReconciliation() {
        return "PENDING".equals(reconciliationStatus) ||
                "MISMATCH".equals(reconciliationStatus);
    }
}