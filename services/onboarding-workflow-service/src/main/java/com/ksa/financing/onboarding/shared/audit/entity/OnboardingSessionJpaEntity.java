package com.ksa.financing.onboarding.shared.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "onboarding_sessions")
public class OnboardingSessionJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "workflow_id", nullable = false, unique = true, length = 200)
    private String workflowId;

    @Column(name = "flow_type", nullable = false, length = 20)
    private String flowType;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(name = "mobile_hash", length = 64)
    private String mobileHash;

    @Column(name = "national_id_hash", length = 64)
    private String nationalIdHash;

    @Column(name = "masked_email", length = 120)
    private String maskedEmail;

    @Column(name = "masked_mobile", length = 60)
    private String maskedMobile;

    @Column(name = "keycloak_user_id", length = 100)
    private String keycloakUserId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "wallet_id", length = 100)
    private String walletId;

    @Column(name = "global_uid", length = 100)
    private String globalUid;

    @Column(name = "current_step", length = 50)
    private String currentStep;

    @Column(name = "onboarding_complete", nullable = false)
    private boolean onboardingComplete;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (startedAt == null) startedAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }

    public String getMobileHash() { return mobileHash; }
    public void setMobileHash(String mobileHash) { this.mobileHash = mobileHash; }

    public String getNationalIdHash() { return nationalIdHash; }
    public void setNationalIdHash(String nationalIdHash) { this.nationalIdHash = nationalIdHash; }

    public String getMaskedEmail() { return maskedEmail; }
    public void setMaskedEmail(String maskedEmail) { this.maskedEmail = maskedEmail; }

    public String getMaskedMobile() { return maskedMobile; }
    public void setMaskedMobile(String maskedMobile) { this.maskedMobile = maskedMobile; }

    public String getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(String keycloakUserId) { this.keycloakUserId = keycloakUserId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getGlobalUid() { return globalUid; }
    public void setGlobalUid(String globalUid) { this.globalUid = globalUid; }

    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

    public boolean isOnboardingComplete() { return onboardingComplete; }
    public void setOnboardingComplete(boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
