package com.ksa.islamic.reporting.readmodel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Denormalized read model for customer portfolio summary.
 * Aggregates all loan data per customer for fast analytics.
 */
@Entity
@Table(name = "customer_portfolio_read_model",
        indexes = {
                @Index(name = "idx_customer_portfolio_id", columnList = "customer_id", unique = true),
                @Index(name = "idx_customer_risk", columnList = "risk_category"),
                @Index(name = "idx_customer_segment", columnList = "customer_segment"),
                @Index(name = "idx_customer_outstanding", columnList = "total_outstanding"),
                @Index(name = "idx_customer_tenant", columnList = "tenant_id"),
                @Index(name = "idx_customer_name_search", columnList = "customer_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CustomerPortfolioReadModel {

    @Id
    @Column(name = "id", nullable = false)
    private String id; // Usually same as customerId for 1:1 mapping

    // Tenant isolation
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Customer identification
    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "customer_segment")
    private String customerSegment; // RETAIL, SME, CORPORATE

    @Column(name = "customer_type")
    private String customerType; // INDIVIDUAL, BUSINESS

    // Portfolio metrics
    @Column(name = "total_loans")
    private Integer totalLoans; // Lifetime loan count

    @Column(name = "active_loans")
    private Integer activeLoans;

    @Column(name = "closed_loans")
    private Integer closedLoans;

    @Column(name = "written_off_loans")
    private Integer writtenOffLoans;

    // Financial aggregates
    @Column(name = "total_disbursed", precision = 19, scale = 4)
    private BigDecimal totalDisbursed; // Lifetime disbursements

    @Column(name = "total_outstanding", precision = 19, scale = 4)
    private BigDecimal totalOutstanding;

    @Column(name = "total_principal_paid", precision = 19, scale = 4)
    private BigDecimal totalPrincipalPaid;

    @Column(name = "total_paid_interest", precision = 19, scale = 4)
    private BigDecimal totalPaidInterest; // Revenue from customer

    @Column(name = "overdue_amount", precision = 19, scale = 4)
    private BigDecimal overdueAmount;

    @Column(name = "total_late_fees", precision = 19, scale = 4)
    private BigDecimal totalLateFees;

    // Current loan details
    @Column(name = "largest_loan_amount", precision = 19, scale = 4)
    private BigDecimal largestLoanAmount;

    @Column(name = "current_installment_amount", precision = 19, scale = 4)
    private BigDecimal currentInstallmentAmount; // Sum of all active loan installments

    @Column(name = "average_loan_size", precision = 19, scale = 4)
    private BigDecimal averageLoanSize;

    @Column(name = "average_interest_rate", precision = 5, scale = 2)
    private BigDecimal averageInterestRate;

    // Dates
    @Column(name = "first_loan_date")
    private LocalDate firstLoanDate;

    @Column(name = "last_loan_date")
    private LocalDate lastLoanDate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "customer_since")
    private LocalDate customerSince;

    // First loan details (for acquisition analysis)
    @Column(name = "first_loan_amount", precision = 19, scale = 4)
    private BigDecimal firstLoanAmount;

    @Column(name = "first_loan_product")
    private String firstLoanProduct;

    // Payment behavior
    @Column(name = "payment_performance_score")
    private Integer paymentPerformanceScore; // 0-100

    @Column(name = "total_payments_made")
    private Integer totalPaymentsMade;

    @Column(name = "on_time_payments")
    private Integer onTimePayments;

    @Column(name = "late_payments")
    private Integer latePayments;

    @Column(name = "bounced_payments")
    private Integer bouncedPayments;

    @Column(name = "max_days_overdue")
    private Integer maxDaysOverdue; // Worst delinquency

    @Column(name = "current_days_overdue")
    private Integer currentDaysOverdue;

    // Risk assessment
    @Column(name = "risk_category")
    private String riskCategory; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "risk_score")
    private Integer riskScore; // 0-1000

    @Column(name = "probability_of_default", precision = 5, scale = 2)
    private BigDecimal probabilityOfDefault; // Percentage

    @Column(name = "exposure_at_default", precision = 19, scale = 4)
    private BigDecimal exposureAtDefault;

    // Collateral
    @Column(name = "total_collateral_value", precision = 19, scale = 4)
    private BigDecimal totalCollateralValue;

    @Column(name = "ltv_ratio", precision = 5, scale = 2)
    private BigDecimal ltvRatio; // Weighted average LTV

    // Customer value metrics
    @Column(name = "lifetime_value", precision = 19, scale = 4)
    private BigDecimal lifetimeValue; // Total revenue from customer

    @Column(name = "profitability_score")
    private Integer profitabilityScore; // 0-100

    // Collection status
    @Column(name = "collection_status")
    private String collectionStatus; // CURRENT, WATCH, COLLECTION, LEGAL

    @Column(name = "last_contact_date")
    private LocalDate lastContactDate;

    @Column(name = "promise_to_pay_date")
    private LocalDate promiseToPayDate;

    // Islamic finance specific
    @Column(name = "preferred_finance_type")
    private String preferredFinanceType; // MURABAHA, TAWARRUQ, IJARA

    @Column(name = "sharia_compliance_verified")
    private Boolean shariaComplianceVerified;

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

    // Computed methods

    /**
     * Calculate the customer tenure in months.
     */
    @Transient
    public int getTenureMonths() {
        if (customerSince == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(customerSince, LocalDate.now());
    }

    /**
     * Calculate on-time payment rate.
     */
    @Transient
    public BigDecimal getOnTimePaymentRate() {
        if (totalPaymentsMade == null || totalPaymentsMade == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(onTimePayments)
                .divide(BigDecimal.valueOf(totalPaymentsMade), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if customer is VIP (high value, low risk).
     */
    @Transient
    public boolean isVip() {
        return "LOW".equals(riskCategory) &&
                totalOutstanding != null &&
                totalOutstanding.compareTo(BigDecimal.valueOf(100000)) > 0;
    }

    /**
     * Check if customer needs attention.
     */
    @Transient
    public boolean needsAttention() {
        return currentDaysOverdue != null && currentDaysOverdue > 30;
    }
}