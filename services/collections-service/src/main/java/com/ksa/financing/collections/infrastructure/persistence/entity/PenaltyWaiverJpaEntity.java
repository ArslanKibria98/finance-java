package com.ksa.financing.collections.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "penalty_waivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PenaltyWaiverJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_id", nullable = false)
    private UUID installmentId;

    @Column(name = "original_penalty", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalPenalty;

    @Column(name = "waived_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal waivedAmount;

    @Column(name = "remaining_penalty", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingPenalty;

    @Column(name = "waiver_type", nullable = false, length = 20)
    private String waiverType;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "approval_reference", length = 100)
    private String approvalReference;

    @Column(name = "waived_by", nullable = false)
    private UUID waivedBy;

    @Column(name = "waived_at", nullable = false)
    private LocalDateTime waivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
