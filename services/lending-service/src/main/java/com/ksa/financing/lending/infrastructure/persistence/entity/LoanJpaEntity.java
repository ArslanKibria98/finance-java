package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class LoanJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_number", nullable = false, unique = true)
    private String loanNumber;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "sharia_structure", nullable = false)
    private String shariaStructure;

    @Column(name = "commodity_transaction_id")
    private UUID commodityTransactionId;

    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "profit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal profitAmount;

    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "profit_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal profitRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "installment_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "outstanding_principal", precision = 18, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "outstanding_profit", precision = 18, scale = 2)
    private BigDecimal outstandingProfit;

    @Column(name = "outstanding_fees", precision = 18, scale = 2)
    private BigDecimal outstandingFees;

    @Column(name = "total_outstanding", precision = 18, scale = 2)
    private BigDecimal totalOutstanding;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "first_due_date")
    private LocalDate firstDueDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "current_dpd")
    private Integer currentDpd;

    @Column(name = "max_dpd")
    private Integer maxDpd;

    @Column(name = "ifrs9_stage")
    private Integer ifrs9Stage;

    @Column(name = "fineract_loan_id")
    private Long fineractLoanId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
