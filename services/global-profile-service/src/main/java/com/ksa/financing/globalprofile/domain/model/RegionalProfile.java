package com.ksa.financing.globalprofile.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class RegionalProfile {
    private UUID regionalProfileId;
    private UUID globalUid;
    private String countryCode;
    private String regionalCifNumber;
    private KycStatus regionalKycStatus;
    private Instant kycVerifiedAt;
    private LocalDate kycExpiryDate;
    private String kycProvider;
    private String piiVaultRegion;
    private UUID piiVaultRecordId;
    private String regionalRiskGrade;
    private Instant riskGradeUpdatedAt;
    private boolean regionalPepFlag;
    private boolean regionalSanctionsFlag;
    private UUID keycloakUserId;
    private boolean active;
    private LocalDate activationDate;
    private LocalDate deactivationDate;
    private String deactivationReason;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getRegionalProfileId() { return regionalProfileId; }
    public void setRegionalProfileId(UUID regionalProfileId) { this.regionalProfileId = regionalProfileId; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getRegionalCifNumber() { return regionalCifNumber; }
    public void setRegionalCifNumber(String regionalCifNumber) { this.regionalCifNumber = regionalCifNumber; }
    public KycStatus getRegionalKycStatus() { return regionalKycStatus; }
    public void setRegionalKycStatus(KycStatus regionalKycStatus) { this.regionalKycStatus = regionalKycStatus; }
    public Instant getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(Instant kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }
    public LocalDate getKycExpiryDate() { return kycExpiryDate; }
    public void setKycExpiryDate(LocalDate kycExpiryDate) { this.kycExpiryDate = kycExpiryDate; }
    public String getKycProvider() { return kycProvider; }
    public void setKycProvider(String kycProvider) { this.kycProvider = kycProvider; }
    public String getPiiVaultRegion() { return piiVaultRegion; }
    public void setPiiVaultRegion(String piiVaultRegion) { this.piiVaultRegion = piiVaultRegion; }
    public UUID getPiiVaultRecordId() { return piiVaultRecordId; }
    public void setPiiVaultRecordId(UUID piiVaultRecordId) { this.piiVaultRecordId = piiVaultRecordId; }
    public String getRegionalRiskGrade() { return regionalRiskGrade; }
    public void setRegionalRiskGrade(String regionalRiskGrade) { this.regionalRiskGrade = regionalRiskGrade; }
    public Instant getRiskGradeUpdatedAt() { return riskGradeUpdatedAt; }
    public void setRiskGradeUpdatedAt(Instant riskGradeUpdatedAt) { this.riskGradeUpdatedAt = riskGradeUpdatedAt; }
    public boolean isRegionalPepFlag() { return regionalPepFlag; }
    public void setRegionalPepFlag(boolean regionalPepFlag) { this.regionalPepFlag = regionalPepFlag; }
    public boolean isRegionalSanctionsFlag() { return regionalSanctionsFlag; }
    public void setRegionalSanctionsFlag(boolean regionalSanctionsFlag) { this.regionalSanctionsFlag = regionalSanctionsFlag; }
    public UUID getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(UUID keycloakUserId) { this.keycloakUserId = keycloakUserId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getActivationDate() { return activationDate; }
    public void setActivationDate(LocalDate activationDate) { this.activationDate = activationDate; }
    public LocalDate getDeactivationDate() { return deactivationDate; }
    public void setDeactivationDate(LocalDate deactivationDate) { this.deactivationDate = deactivationDate; }
    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String deactivationReason) { this.deactivationReason = deactivationReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
