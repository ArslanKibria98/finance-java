package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_reschedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRescheduleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "loan_number")
    private String loanNumber;

    @Column(name = "reschedule_type", nullable = false, length = 50)
    private String rescheduleType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "requested_skip_month")
    private LocalDate requestedSkipMonth;

    @Column(name = "extension_months")
    private Integer extensionMonths;

    @Column(name = "holiday_months")
    private Integer holidayMonths;

    @Column(name = "new_profit_rate", precision = 10, scale = 8)
    private BigDecimal newProfitRate;

    @Column(name = "write_off_amount", precision = 19, scale = 6)
    private BigDecimal writeOffAmount;

    @Column(name = "profit_waiver_amount", precision = 19, scale = 6)
    private BigDecimal profitWaiverAmount;

    @Column(name = "old_tenure_months")
    private Integer oldTenureMonths;

    @Column(name = "new_tenure_months")
    private Integer newTenureMonths;

    @Column(name = "old_installment_amount", precision = 19, scale = 6)
    private BigDecimal oldInstallmentAmount;

    @Column(name = "new_installment_amount", precision = 19, scale = 6)
    private BigDecimal newInstallmentAmount;

    @Column(name = "old_maturity_date")
    private LocalDate oldMaturityDate;

    @Column(name = "new_maturity_date")
    private LocalDate newMaturityDate;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "approver_role", length = 100)
    private String approverRole;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "gl_entry_id")
    private UUID glEntryId;

    @Column(name = "gl_posted")
    private boolean glPosted;

    @Column(name = "fineract_reschedule_id")
    private Long fineractRescheduleId;

    @Column(name = "fineract_synced")
    private boolean fineractSynced;

    @Column(name = "workflow_id", length = 255)
    private String workflowId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Version
    @Column(name = "version")
    private int version;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
