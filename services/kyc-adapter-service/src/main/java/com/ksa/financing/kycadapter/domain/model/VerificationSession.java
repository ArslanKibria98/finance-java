package com.ksa.financing.kycadapter.domain.model;

import com.ksa.financing.domain.valueobject.NationalId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class VerificationSession {
    private UUID id;
    private UUID tenantId;
    private String sessionNumber;
    private UUID customerId;
    private UUID globalUid;
    private UUID loanApplicationId;
    private String countryCode;
    private VerificationType verificationType;
    private KycProvider provider;
    private NationalId nationalId;
    private String iqamaNumber;
    private String commercialRegistration;
    private LocalDate dateOfBirth;
    private String fullNameAr;
    private String fullNameEn;
    private SessionStatus status;
    private String providerSessionId;
    private String providerRequestId;
    private VerificationResult result;
    private BigDecimal confidenceScore;
    private Instant initiatedAt;
    private Instant userActionAt;
    private Instant completedAt;
    private Instant expiresAt;
    private int attemptCount;
    private int maxAttempts;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(String sessionNumber) { this.sessionNumber = sessionNumber; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public UUID getLoanApplicationId() { return loanApplicationId; }
    public void setLoanApplicationId(UUID loanApplicationId) { this.loanApplicationId = loanApplicationId; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public VerificationType getVerificationType() { return verificationType; }
    public void setVerificationType(VerificationType verificationType) { this.verificationType = verificationType; }
    public KycProvider getProvider() { return provider; }
    public void setProvider(KycProvider provider) { this.provider = provider; }
    public NationalId getNationalId() { return nationalId; }
    public void setNationalId(NationalId nationalId) { this.nationalId = nationalId; }
    public String getIqamaNumber() { return iqamaNumber; }
    public void setIqamaNumber(String iqamaNumber) { this.iqamaNumber = iqamaNumber; }
    public String getCommercialRegistration() { return commercialRegistration; }
    public void setCommercialRegistration(String commercialRegistration) { this.commercialRegistration = commercialRegistration; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getFullNameAr() { return fullNameAr; }
    public void setFullNameAr(String fullNameAr) { this.fullNameAr = fullNameAr; }
    public String getFullNameEn() { return fullNameEn; }
    public void setFullNameEn(String fullNameEn) { this.fullNameEn = fullNameEn; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public String getProviderSessionId() { return providerSessionId; }
    public void setProviderSessionId(String providerSessionId) { this.providerSessionId = providerSessionId; }
    public String getProviderRequestId() { return providerRequestId; }
    public void setProviderRequestId(String providerRequestId) { this.providerRequestId = providerRequestId; }
    public VerificationResult getResult() { return result; }
    public void setResult(VerificationResult result) { this.result = result; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
    public Instant getUserActionAt() { return userActionAt; }
    public void setUserActionAt(Instant userActionAt) { this.userActionAt = userActionAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
