package com.ksa.islamic.reporting.readmodel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Denormalized read model for loan summary data.
 * Optimized for fast queries with all relevant data in a single table.
 */
@Entity
@Table(name = "loan_summary_read_model",
        indexes = {
                @Index(name = "idx_loan_customer", columnList = "customer_id"),
                @Index(name = "idx_loan_status", columnList = "status"),
                @Index(name = "idx_loan_product", columnList = "product_type"),
                @Index(name = "idx_loan_overdue", columnList = "days_overdue"),
                @Index(name = "idx_loan_maturity", columnList = "maturity_date"),
                @Index(name = "idx_loan_tenant", columnList = "tenant_id"),
                @Index(name = "idx_loan_customer_name", columnList = "customer_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LoanSummaryReadModel {

    @Id
    @Column(name = "loan_id", nullable = false)
    private String loanId;

    // Tenant isolation
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Core loan data
    @Column(name = "loan_account_number", nullable = false, unique = true)
    private String loanAccountNumber;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "product_name")
    private String productName; // Denormalized from product

    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, DISBURSED, ACTIVE, CLOSED, WRITTEN_OFF

    // Customer data (denormalized)
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName; // Denormalized for search

    @Column(name = "national_id")
    private String nationalId; // Denormalized for search

    @Column(name = "customer_segment")
    private String customerSegment; // RETAIL, SME, CORPORATE

    // Financial data
    @Column(name = "principal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "outstanding_principal", precision = 19, scale = 4, nullable = false)
    private BigDecimal outstandingPrincipal;

    @Column(name = "interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "total_interest", precision = 19, scale = 4)
    private BigDecimal totalInterest;

    @Column(name = "paid_principal", precision = 19, scale = 4)
    private BigDecimal paidPrincipal;

    @Column(name = "paid_interest", precision = 19, scale = 4)
    private BigDecimal paidInterest;

    @Column(name = "late_fees", precision = 19, scale = 4)
    private BigDecimal lateFees;

    // Dates
    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    // Islamic finance specific
    @Column(name = "sharia_compliance_type")
    private String shariaComplianceType; // MURABAHA, TAWARRUQ, IJARA

    @Column(name = "commodity_type")
    private String commodityType;

    @Column(name = "profit_rate", precision = 5, scale = 2)
    private BigDecimal profitRate;

    // Payment schedule
    @Column(name = "payment_frequency")
    private String paymentFrequency; // MONTHLY, QUARTERLY, SEMI_ANNUAL

    @Column(name = "installment_amount", precision = 19, scale = 4)
    private BigDecimal installmentAmount;

    @Column(name = "number_of_installments")
    private Integer numberOfInstallments;

    @Column(name = "installments_paid")
    private Integer installmentsPaid;

    // Delinquency tracking
    @Column(name = "days_overdue")
    private Integer daysOverdue;

    @Column(name = "overdue_amount", precision = 19, scale = 4)
    private BigDecimal overdueAmount;

    @Column(name = "overdue_installments")
    private Integer overdueInstallments;

    // Risk metrics
    @Column(name = "risk_category")
    private String riskCategory; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "provision_amount", precision = 19, scale = 4)
    private BigDecimal provisionAmount;

    @Column(name = "collateral_value", precision = 19, scale = 4)
    private BigDecimal collateralValue;

    @Column(name = "ltv_ratio", precision = 5, scale = 2)
    private BigDecimal ltvRatio; // Loan-to-Value ratio

    // Performance metrics
    @Column(name = "payment_performance_score")
    private Integer paymentPerformanceScore; // 0-100

    @Column(name = "early_payments")
    private Integer earlyPayments;

    @Column(name = "late_payments")
    private Integer latePayments;

    // Additional metadata
    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "officer_id")
    private String officerId;

    @Column(name = "officer_name")
    private String officerName; // Denormalized

    @Column(name = "collection_status")
    private String collectionStatus; // CURRENT, REMINDER_SENT, IN_COLLECTION, LEGAL

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Computed fields (for convenience)

    /**
     * Calculate the remaining term in months.
     */
    @Transient
    public int getRemainingTermMonths() {
        if (maturityDate == null || LocalDate.now().isAfter(maturityDate)) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.now(), maturityDate);
    }

    /**
     * Calculate the completion percentage.
     */
    @Transient
    public BigDecimal getCompletionPercentage() {
        if (numberOfInstallments == null || numberOfInstallments == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(installmentsPaid)
                .divide(BigDecimal.valueOf(numberOfInstallments), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if loan is non-performing (NPL).
     */
    @Transient
    public boolean isNonPerforming() {
        return daysOverdue != null && daysOverdue > 90;
    }
}