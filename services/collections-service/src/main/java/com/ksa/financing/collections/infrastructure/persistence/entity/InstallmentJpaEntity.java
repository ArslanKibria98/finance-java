package com.ksa.financing.collections.infrastructure.persistence.entity;

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
@Table(name = "installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class InstallmentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private RepaymentScheduleJpaEntity schedule;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal principalAmount;

    @Column(name = "profit_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal profitAmount;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal feeAmount;

    @Column(name = "late_penalty_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal latePenaltyAmount;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalAmount;

    @Column(name = "paid_principal", nullable = false, precision = 20, scale = 6)
    private BigDecimal paidPrincipal;

    @Column(name = "paid_profit", nullable = false, precision = 20, scale = 6)
    private BigDecimal paidProfit;

    @Column(name = "paid_fee", nullable = false, precision = 20, scale = 6)
    private BigDecimal paidFee;

    @Column(name = "paid_total", nullable = false, precision = 20, scale = 6)
    private BigDecimal paidTotal;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "dpd", nullable = false)
    private int dpd;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "is_eligible_for_writeoff", nullable = false)
    private boolean eligibleForWriteOff;

    @Column(name = "eligibility_evaluated_at")
    private LocalDateTime eligibilityEvaluatedAt;

    @Column(name = "waived_penalty_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal waivedPenaltyAmount;

    @Column(name = "written_off_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal writtenOffPrincipal;

    @Column(name = "written_off_profit", nullable = false, precision = 19, scale = 4)
    private BigDecimal writtenOffProfit;

    @Column(name = "written_off_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal writtenOffFee;

    @Column(name = "written_off_penalty", nullable = false, precision = 19, scale = 4)
    private BigDecimal writtenOffPenalty;

    @Column(name = "write_off_date")
    private LocalDate writeOffDate;

    @Column(name = "write_off_reason", length = 500)
    private String writeOffReason;

    @Column(name = "written_off_by")
    private UUID writtenOffBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
