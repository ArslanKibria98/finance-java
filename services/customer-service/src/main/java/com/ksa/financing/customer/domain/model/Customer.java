package com.ksa.financing.customer.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Customer {
    private UUID id;
    private UUID tenantId;
    private String cifNumber;
    private CustomerType customerType;
    private String nationalId;
    private String nationalIdType;
    private String title;
    private String firstName;
    private String middleName;
    private String lastName;
    private String firstNameAr;
    private String lastNameAr;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String nationality;
    private ResidencyType residencyType;
    private String mobileNumber;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String postalCode;
    private String country;
    private KycStatus kycStatus;
    private Instant kycVerifiedAt;
    private LocalDate kycExpiryDate;
    private boolean nafathVerified;
    private String nafathTransactionId;
    private RiskGrade riskGrade;
    private Instant riskGradeUpdatedAt;
    private boolean pepFlag;
    private boolean sanctionsFlag;
    private PepStatus pepStatus;
    private UUID keycloakUserId;
    private UUID globalUid;
    private LifecycleStage lifecycleStage;
    private Instant lifecycleStageChangedAt;
    private String customerSegment;
    private String onboardingFlow;
    private String acquisitionChannel;
    private UUID acquisitionPartnerId;
    private boolean active;
    private Instant blockedAt;
    private String blockedReason;
    private String profilePicture;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private java.util.List<String> blockCodes = new java.util.ArrayList<>();

    public boolean isBlocked() {
        return !active;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCifNumber() { return cifNumber; }
    public void setCifNumber(String cifNumber) { this.cifNumber = cifNumber; }
    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getNationalIdType() { return nationalIdType; }
    public void setNationalIdType(String nationalIdType) { this.nationalIdType = nationalIdType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstNameAr() { return firstNameAr; }
    public void setFirstNameAr(String firstNameAr) { this.firstNameAr = firstNameAr; }
    public String getLastNameAr() { return lastNameAr; }
    public void setLastNameAr(String lastNameAr) { this.lastNameAr = lastNameAr; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public ResidencyType getResidencyType() { return residencyType; }
    public void setResidencyType(ResidencyType residencyType) { this.residencyType = residencyType; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }
    public Instant getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(Instant kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }
    public LocalDate getKycExpiryDate() { return kycExpiryDate; }
    public void setKycExpiryDate(LocalDate kycExpiryDate) { this.kycExpiryDate = kycExpiryDate; }
    public boolean isNafathVerified() { return nafathVerified; }
    public void setNafathVerified(boolean nafathVerified) { this.nafathVerified = nafathVerified; }
    public String getNafathTransactionId() { return nafathTransactionId; }
    public void setNafathTransactionId(String nafathTransactionId) { this.nafathTransactionId = nafathTransactionId; }
    public RiskGrade getRiskGrade() { return riskGrade; }
    public void setRiskGrade(RiskGrade riskGrade) { this.riskGrade = riskGrade; }
    public Instant getRiskGradeUpdatedAt() { return riskGradeUpdatedAt; }
    public void setRiskGradeUpdatedAt(Instant riskGradeUpdatedAt) { this.riskGradeUpdatedAt = riskGradeUpdatedAt; }
    public boolean isPepFlag() { return pepFlag; }
    public void setPepFlag(boolean pepFlag) { this.pepFlag = pepFlag; }
    public boolean isSanctionsFlag() { return sanctionsFlag; }
    public void setSanctionsFlag(boolean sanctionsFlag) { this.sanctionsFlag = sanctionsFlag; }
    public PepStatus getPepStatus() { return pepStatus; }
    public void setPepStatus(PepStatus pepStatus) { this.pepStatus = pepStatus; }
    public UUID getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(UUID keycloakUserId) { this.keycloakUserId = keycloakUserId; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public LifecycleStage getLifecycleStage() { return lifecycleStage; }
    public void setLifecycleStage(LifecycleStage lifecycleStage) { this.lifecycleStage = lifecycleStage; }
    public Instant getLifecycleStageChangedAt() { return lifecycleStageChangedAt; }
    public void setLifecycleStageChangedAt(Instant lifecycleStageChangedAt) { this.lifecycleStageChangedAt = lifecycleStageChangedAt; }
    public String getCustomerSegment() { return customerSegment; }
    public void setCustomerSegment(String customerSegment) { this.customerSegment = customerSegment; }
    public String getOnboardingFlow() { return onboardingFlow; }
    public void setOnboardingFlow(String onboardingFlow) { this.onboardingFlow = onboardingFlow; }
    public String getAcquisitionChannel() { return acquisitionChannel; }
    public void setAcquisitionChannel(String acquisitionChannel) { this.acquisitionChannel = acquisitionChannel; }
    public UUID getAcquisitionPartnerId() { return acquisitionPartnerId; }
    public void setAcquisitionPartnerId(UUID acquisitionPartnerId) { this.acquisitionPartnerId = acquisitionPartnerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public java.util.List<String> getBlockCodes() { return blockCodes; }
    public void setBlockCodes(java.util.List<String> blockCodes) { this.blockCodes = blockCodes; }
}
