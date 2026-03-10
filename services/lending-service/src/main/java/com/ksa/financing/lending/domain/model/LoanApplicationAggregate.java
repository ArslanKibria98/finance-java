package com.ksa.financing.lending.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for loan applications. Enforces all business invariants
 * and emits domain events on state changes. Zero framework imports.
 */
public class LoanApplicationAggregate {

    private final LoanApplicationId id;
    private final UUID tenantId;
    private final String applicationNumber;
    private final UUID customerId;
    private final UUID productId;
    private final String productCode;
    private final ShariaStructure shariaStructure;
    private final BigDecimal requestedAmount;
    private final int requestedTenureMonths;

    private UUID partnerId;
    private UUID leadId;

    // Approved terms
    private BigDecimal approvedAmount;
    private Integer approvedTenureMonths;
    private BigDecimal approvedProfitRate;

    // Calculated amounts
    private BigDecimal totalProfit;
    private BigDecimal totalRepayment;
    private BigDecimal monthlyInstallment;

    // DBR
    private BigDecimal dbrBefore;
    private BigDecimal dbrAfter;

    // Status
    private ApplicationStatus status;

    // Workflow
    private String workflowId;
    private String currentStage;

    // Timing
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;

    // Idempotency
    private String idempotencyKey;

    // Audit
    private final LocalDateTime createdAt;
    private final UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private int version;

    // Domain events
    private final List<Object> uncommittedEvents = new ArrayList<>();

    private LoanApplicationAggregate(LoanApplicationId id, UUID tenantId, String applicationNumber,
                                      UUID customerId, UUID productId, String productCode,
                                      ShariaStructure shariaStructure, BigDecimal requestedAmount,
                                      int requestedTenureMonths, UUID createdBy,
                                      LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.applicationNumber = applicationNumber;
        this.customerId = customerId;
        this.productId = productId;
        this.productCode = productCode;
        this.shariaStructure = shariaStructure;
        this.requestedAmount = requestedAmount;
        this.requestedTenureMonths = requestedTenureMonths;
        this.status = ApplicationStatus.DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = this.createdAt;
        this.createdBy = createdBy;
        this.version = 1;
    }

    // ==================== FACTORY METHOD ====================

    public static LoanApplicationAggregate create(UUID tenantId, String applicationNumber,
                                                   UUID customerId, UUID productId, String productCode,
                                                   ShariaStructure shariaStructure, BigDecimal requestedAmount,
                                                   int requestedTenureMonths, UUID createdBy) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (applicationNumber == null || applicationNumber.isBlank())
            throw new IllegalArgumentException("Application number cannot be empty");
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        if (productId == null) throw new IllegalArgumentException("Product ID cannot be null");
        if (shariaStructure == null) throw new IllegalArgumentException("Sharia structure cannot be null");
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Requested amount must be positive");
        if (requestedTenureMonths <= 0)
            throw new IllegalArgumentException("Requested tenure must be positive");

        var aggregate = new LoanApplicationAggregate(
                LoanApplicationId.generate(), tenantId, applicationNumber,
                customerId, productId, productCode, shariaStructure,
                requestedAmount, requestedTenureMonths, createdBy, LocalDateTime.now()
        );

        aggregate.registerEvent(new LoanApplicationCreated(
                aggregate.id, tenantId, customerId, productId,
                shariaStructure, requestedAmount, requestedTenureMonths
        ));

        return aggregate;
    }

    // ==================== RECONSTITUTION (from persistence) ====================

    public static LoanApplicationAggregate reconstitute(
            LoanApplicationId id, UUID tenantId, String applicationNumber,
            UUID customerId, UUID productId, String productCode,
            ShariaStructure shariaStructure, BigDecimal requestedAmount,
            int requestedTenureMonths, UUID partnerId, UUID leadId,
            BigDecimal approvedAmount, Integer approvedTenureMonths,
            BigDecimal approvedProfitRate, BigDecimal totalProfit,
            BigDecimal totalRepayment, BigDecimal monthlyInstallment,
            BigDecimal dbrBefore, BigDecimal dbrAfter,
            ApplicationStatus status, String workflowId, String currentStage,
            LocalDateTime submittedAt, LocalDateTime expiresAt,
            String idempotencyKey,
            UUID createdBy, LocalDateTime createdAt,
            UUID updatedBy, LocalDateTime updatedAt, int version) {

        var agg = new LoanApplicationAggregate(
                id, tenantId, applicationNumber, customerId, productId, productCode,
                shariaStructure, requestedAmount, requestedTenureMonths, createdBy, createdAt);

        agg.partnerId = partnerId;
        agg.leadId = leadId;
        agg.approvedAmount = approvedAmount;
        agg.approvedTenureMonths = approvedTenureMonths;
        agg.approvedProfitRate = approvedProfitRate;
        agg.totalProfit = totalProfit;
        agg.totalRepayment = totalRepayment;
        agg.monthlyInstallment = monthlyInstallment;
        agg.dbrBefore = dbrBefore;
        agg.dbrAfter = dbrAfter;
        agg.status = status;
        agg.workflowId = workflowId;
        agg.currentStage = currentStage;
        agg.submittedAt = submittedAt;
        agg.expiresAt = expiresAt;
        agg.idempotencyKey = idempotencyKey;
        agg.updatedBy = updatedBy;
        agg.updatedAt = updatedAt;
        agg.version = version;
        // Do NOT register any domain events on reconstitution
        return agg;
    }

    // ==================== BUSINESS OPERATIONS ====================

    public void submit(UUID submittedBy) {
        assertTransition(ApplicationStatus.SUBMITTED);
        this.status = ApplicationStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedBy = submittedBy;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new LoanApplicationSubmitted(id, tenantId, customerId));
    }

    public void moveToDocumentsPending(UUID updatedBy) {
        assertTransition(ApplicationStatus.DOCUMENTS_PENDING);
        this.status = ApplicationStatus.DOCUMENTS_PENDING;
        this.currentStage = "DOCUMENTS_PENDING";
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveToUnderReview(UUID updatedBy) {
        assertTransition(ApplicationStatus.UNDER_REVIEW);
        this.status = ApplicationStatus.UNDER_REVIEW;
        this.currentStage = "UNDER_REVIEW";
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveToCreditCheck(UUID updatedBy) {
        assertTransition(ApplicationStatus.CREDIT_CHECK);
        this.status = ApplicationStatus.CREDIT_CHECK;
        this.currentStage = "CREDIT_CHECK";
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordCreditCheckResult(BigDecimal dbrBefore, BigDecimal dbrAfter) {
        if (this.status != ApplicationStatus.CREDIT_CHECK)
            throw new IllegalStateException("Can only record credit check result in CREDIT_CHECK status");
        this.dbrBefore = dbrBefore;
        this.dbrAfter = dbrAfter;
    }

    public void moveToShariaValidation(UUID updatedBy) {
        assertTransition(ApplicationStatus.SHARIA_VALIDATION);
        this.status = ApplicationStatus.SHARIA_VALIDATION;
        this.currentStage = "SHARIA_VALIDATION";
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveToPendingApproval(UUID updatedBy) {
        assertTransition(ApplicationStatus.PENDING_APPROVAL);
        this.status = ApplicationStatus.PENDING_APPROVAL;
        this.currentStage = "PENDING_APPROVAL";
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(BigDecimal approvedAmount, int approvedTenureMonths,
                         BigDecimal approvedProfitRate, BigDecimal totalProfit,
                         BigDecimal totalRepayment, BigDecimal monthlyInstallment,
                         UUID approvedBy) {
        assertTransition(ApplicationStatus.APPROVED);
        if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Approved amount must be positive");

        this.status = ApplicationStatus.APPROVED;
        this.approvedAmount = approvedAmount;
        this.approvedTenureMonths = approvedTenureMonths;
        this.approvedProfitRate = approvedProfitRate;
        this.totalProfit = totalProfit;
        this.totalRepayment = totalRepayment;
        this.monthlyInstallment = monthlyInstallment;
        this.currentStage = "APPROVED";
        this.updatedBy = approvedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new LoanApplicationApproved(
                id, tenantId, customerId, productId,
                approvedAmount, approvedTenureMonths, approvedProfitRate
        ));
    }

    public void reject(String reason, UUID rejectedBy) {
        assertTransition(ApplicationStatus.REJECTED);
        this.status = ApplicationStatus.REJECTED;
        this.currentStage = "REJECTED";
        this.updatedBy = rejectedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new LoanApplicationRejected(id, tenantId, customerId, reason));
    }

    public void cancel(UUID cancelledBy) {
        assertTransition(ApplicationStatus.CANCELLED);
        this.status = ApplicationStatus.CANCELLED;
        this.currentStage = "CANCELLED";
        this.updatedBy = cancelledBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignWorkflow(String workflowId) {
        this.workflowId = workflowId;
    }

    public void setPartner(UUID partnerId, UUID leadId) {
        this.partnerId = partnerId;
        this.leadId = leadId;
    }

    // ==================== EVENT MANAGEMENT ====================

    private void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // ==================== ASSERTIONS ====================

    private void assertTransition(ApplicationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition from " + status + " to " + target);
        }
    }

    // ==================== GETTERS ====================

    public LoanApplicationId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getApplicationNumber() { return applicationNumber; }
    public UUID getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public String getProductCode() { return productCode; }
    public ShariaStructure getShariaStructure() { return shariaStructure; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public int getRequestedTenureMonths() { return requestedTenureMonths; }
    public UUID getPartnerId() { return partnerId; }
    public UUID getLeadId() { return leadId; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public Integer getApprovedTenureMonths() { return approvedTenureMonths; }
    public BigDecimal getApprovedProfitRate() { return approvedProfitRate; }
    public BigDecimal getTotalProfit() { return totalProfit; }
    public BigDecimal getTotalRepayment() { return totalRepayment; }
    public BigDecimal getMonthlyInstallment() { return monthlyInstallment; }
    public BigDecimal getDbrBefore() { return dbrBefore; }
    public BigDecimal getDbrAfter() { return dbrAfter; }
    public ApplicationStatus getStatus() { return status; }
    public String getWorkflowId() { return workflowId; }
    public String getCurrentStage() { return currentStage; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public int getVersion() { return version; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    // ==================== DOMAIN EVENTS (records) ====================

    public record LoanApplicationCreated(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            UUID productId, ShariaStructure shariaStructure,
            BigDecimal requestedAmount, int requestedTenureMonths
    ) {}

    public record LoanApplicationSubmitted(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId
    ) {}

    public record LoanApplicationApproved(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            UUID productId, BigDecimal approvedAmount,
            int approvedTenureMonths, BigDecimal approvedProfitRate
    ) {}

    public record LoanApplicationRejected(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            String reason
    ) {}
}
