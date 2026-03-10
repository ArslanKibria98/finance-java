package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class LoanApplicationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "application_number", nullable = false, unique = true)
    private String applicationNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "sharia_structure", nullable = false)
    private String shariaStructure;

    @Column(name = "requested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_tenure_months", nullable = false)
    private Integer requestedTenureMonths;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "approved_amount", precision = 18, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "approved_tenure_months")
    private Integer approvedTenureMonths;

    @Column(name = "approved_profit_rate", precision = 8, scale = 4)
    private BigDecimal approvedProfitRate;

    @Column(name = "total_profit", precision = 18, scale = 2)
    private BigDecimal totalProfit;

    @Column(name = "total_repayment", precision = 18, scale = 2)
    private BigDecimal totalRepayment;

    @Column(name = "monthly_installment", precision = 18, scale = 2)
    private BigDecimal monthlyInstallment;

    @Column(name = "dbr_before", precision = 8, scale = 4)
    private BigDecimal dbrBefore;

    @Column(name = "dbr_after", precision = 8, scale = 4)
    private BigDecimal dbrAfter;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "workflow_id")
    private String workflowId;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
