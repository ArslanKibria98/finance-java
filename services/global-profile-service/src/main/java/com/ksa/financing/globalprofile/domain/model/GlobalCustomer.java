package com.ksa.financing.globalprofile.domain.model;

import java.time.Instant;
import java.util.UUID;

public class GlobalCustomer {
    private UUID globalUid;
    private CustomerType customerType;
    private String globalEmailHash;
    private String globalMobileHash;
    private String primaryCountryCode;
    private GlobalKycAggregateStatus globalKycStatus;
    private Instant kycLastVerifiedAt;
    private String globalRiskGrade;
    private Instant globalRiskUpdatedAt;
    private boolean pepFlag;
    private boolean sanctionsFlag;
    private boolean fraudFlag;
    private boolean active;
    private Instant blockedAt;
    private String blockedReason;
    private String acquisitionSource;
    private String customerSegment;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }
    public String getGlobalEmailHash() { return globalEmailHash; }
    public void setGlobalEmailHash(String globalEmailHash) { this.globalEmailHash = globalEmailHash; }
    public String getGlobalMobileHash() { return globalMobileHash; }
    public void setGlobalMobileHash(String globalMobileHash) { this.globalMobileHash = globalMobileHash; }
    public String getPrimaryCountryCode() { return primaryCountryCode; }
    public void setPrimaryCountryCode(String primaryCountryCode) { this.primaryCountryCode = primaryCountryCode; }
    public GlobalKycAggregateStatus getGlobalKycStatus() { return globalKycStatus; }
    public void setGlobalKycStatus(GlobalKycAggregateStatus globalKycStatus) { this.globalKycStatus = globalKycStatus; }
    public Instant getKycLastVerifiedAt() { return kycLastVerifiedAt; }
    public void setKycLastVerifiedAt(Instant kycLastVerifiedAt) { this.kycLastVerifiedAt = kycLastVerifiedAt; }
    public String getGlobalRiskGrade() { return globalRiskGrade; }
    public void setGlobalRiskGrade(String globalRiskGrade) { this.globalRiskGrade = globalRiskGrade; }
    public Instant getGlobalRiskUpdatedAt() { return globalRiskUpdatedAt; }
    public void setGlobalRiskUpdatedAt(Instant globalRiskUpdatedAt) { this.globalRiskUpdatedAt = globalRiskUpdatedAt; }
    public boolean isPepFlag() { return pepFlag; }
    public void setPepFlag(boolean pepFlag) { this.pepFlag = pepFlag; }
    public boolean isSanctionsFlag() { return sanctionsFlag; }
    public void setSanctionsFlag(boolean sanctionsFlag) { this.sanctionsFlag = sanctionsFlag; }
    public boolean isFraudFlag() { return fraudFlag; }
    public void setFraudFlag(boolean fraudFlag) { this.fraudFlag = fraudFlag; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public String getAcquisitionSource() { return acquisitionSource; }
    public void setAcquisitionSource(String acquisitionSource) { this.acquisitionSource = acquisitionSource; }
    public String getCustomerSegment() { return customerSegment; }
    public void setCustomerSegment(String customerSegment) { this.customerSegment = customerSegment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
