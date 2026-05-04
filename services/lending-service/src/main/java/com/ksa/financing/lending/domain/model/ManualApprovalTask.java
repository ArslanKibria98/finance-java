package com.ksa.financing.lending.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ManualApprovalTask {

    public enum Status {
        PENDING, APPROVED, REJECTED, BREACHED, EXPIRED
    }

    private UUID id;
    private UUID tenantId;
    private UUID applicationId;
    private String applicationNumber;
    private UUID customerId;
    private String customerName;
    private UUID productId;
    private String productName;
    private BigDecimal requestedAmount;
    private int tenureMonths;
    private BigDecimal monthlyInstallment;
    private Integer creditScore;
    private BigDecimal dbrPercentage;
    private String assignedRole;
    private Status status;
    private OffsetDateTime slaDeadline;
    private boolean slaBreached;
    private UUID decisionBy;
    private OffsetDateTime decisionAt;
    private String decisionNotes;
    private String rejectionReason;
    private String workflowId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private int version;

    public void approve(UUID decisionBy, String notes) {
        if (this.status != Status.PENDING && this.status != Status.BREACHED) {
            throw new IllegalStateException("Cannot approve task in status: " + this.status);
        }
        this.status = Status.APPROVED;
        this.decisionBy = decisionBy;
        this.decisionAt = OffsetDateTime.now();
        this.decisionNotes = notes;
    }

    public void reject(UUID decisionBy, String reason, String notes) {
        if (this.status != Status.PENDING && this.status != Status.BREACHED) {
            throw new IllegalStateException("Cannot reject task in status: " + this.status);
        }
        this.status = Status.REJECTED;
        this.decisionBy = decisionBy;
        this.decisionAt = OffsetDateTime.now();
        this.rejectionReason = reason;
        this.decisionNotes = notes;
    }

    public void markBreached() {
        this.slaBreached = true;
        if (this.status == Status.PENDING) {
            this.status = Status.BREACHED;
        }
    }
}
