package com.ksa.financing.collections.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "repayment_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RepaymentScheduleJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "schedule_number", nullable = false)
    private String scheduleNumber;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "total_installments", nullable = false)
    private int totalInstallments;

    @Column(name = "total_principal", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalPrincipal;

    @Column(name = "total_profit", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalProfit;

    @Column(name = "total_fee", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalFee;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalAmount;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Column(name = "last_due_date", nullable = false)
    private LocalDate lastDueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InstallmentJpaEntity> installments = new ArrayList<>();
}
