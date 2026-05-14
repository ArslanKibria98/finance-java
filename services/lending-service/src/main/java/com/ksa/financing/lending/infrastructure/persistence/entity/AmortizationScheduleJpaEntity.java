package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "amortization_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmortizationScheduleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "schedule_version", nullable = false)
    private int scheduleVersion;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "opening_principal", nullable = false, precision = 20, scale = 6)
    private BigDecimal openingPrincipal;

    @Column(name = "principal_component", nullable = false, precision = 20, scale = 6)
    private BigDecimal principalComponent;

    @Column(name = "profit_component", nullable = false, precision = 20, scale = 6)
    private BigDecimal profitComponent;

    @Column(name = "fee_component", nullable = false, precision = 20, scale = 6)
    private BigDecimal feeComponent;

    @Column(name = "total_installment", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalInstallment;

    @Column(name = "closing_principal", nullable = false, precision = 20, scale = 6)
    private BigDecimal closingPrincipal;

    @Column(name = "cumulative_principal", nullable = false, precision = 20, scale = 6)
    private BigDecimal cumulativePrincipal;

    @Column(name = "cumulative_profit", nullable = false, precision = 20, scale = 6)
    private BigDecimal cumulativeProfit;

    @Column(name = "calculation_method", nullable = false, length = 50)
    private String calculationMethod;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;   // PENDING, PAID, SKIPPED, DEFERRED

    @Column(name = "is_skipped")
    private Boolean skipped;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = "PENDING";
        if (skipped == null) skipped = false;
    }
}
