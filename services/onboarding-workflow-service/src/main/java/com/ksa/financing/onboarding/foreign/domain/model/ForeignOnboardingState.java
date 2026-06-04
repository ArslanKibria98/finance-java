package com.ksa.financing.onboarding.foreign.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForeignOnboardingState implements Serializable {

    private static final long serialVersionUID = 1L;

    private ForeignOnboardingStep currentStep;
    private String workflowId;
    private String email;
    private String mobileNumber;
    private String tenantId;
    private String countryOfOrigin;
    private String residentialCountry;
    private String initialDeviceId;

    private String mobileOtpRequestId;
    private String emailOtpRequestId;

    private String passportImageBase64;
    private String faciaPassportReferenceId;
    private Map<String, Object> extractedData;
    private Map<String, Object> confirmedData;
    private String passportIssueDate;
    private String passportExpiryDate;

    private String faciaFaceMatchReferenceId;
    private Double faceMatchScore;

    // Sullis KYC session — created at passport upload, reused at selfie/submit.
    // Stored per workflow so concurrent onboardings never cross sessions.
    private String sullisSessionId;
    private String sullisAttemptId;

    private String customerId;
    private String walletId;
    private String keycloakUserId;
    private String globalUid;

    private String accessToken;
    private String refreshToken;
    private long tokenExpiresIn;
    private String tokenType;

    private boolean biometricsEnabled;
    private boolean biometricsSkipped;
    private boolean pinSet;

    /**
     * Flips to {@code true} once PIN setup succeeds. Foreign nationals who finish the
     * full passport + selfie + PIN journey ARE fully onboarded (unlike guest users
     * whose flag stays {@code false}).
     */
    private boolean onboardingComplete;

    private String failureReason;
    private Instant startedAt;
    private Instant lastUpdatedAt;

    // Retry counters — incremented every time the workflow processes a signal for
    // the corresponding step. Used by the use-case services to detect when a
    // signal-processing attempt completed, so they can report decline reasons
    // without needing the workflow to terminate.
    private int otpAttempts;
    private int passportAttempts;
    private int selfieAttempts;

    public ForeignOnboardingState() {
        this.extractedData = new LinkedHashMap<>();
        this.confirmedData = new LinkedHashMap<>();
        this.onboardingComplete = false;
    }

    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }

    public int getPassportAttempts() { return passportAttempts; }
    public void setPassportAttempts(int passportAttempts) { this.passportAttempts = passportAttempts; }

    public int getSelfieAttempts() { return selfieAttempts; }
    public void setSelfieAttempts(int selfieAttempts) { this.selfieAttempts = selfieAttempts; }

    public ForeignOnboardingStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(ForeignOnboardingStep currentStep) { this.currentStep = currentStep; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getResidentialCountry() { return residentialCountry; }
    public void setResidentialCountry(String residentialCountry) { this.residentialCountry = residentialCountry; }

    public String getInitialDeviceId() { return initialDeviceId; }
    public void setInitialDeviceId(String initialDeviceId) { this.initialDeviceId = initialDeviceId; }

    public String getMobileOtpRequestId() { return mobileOtpRequestId; }
    public void setMobileOtpRequestId(String mobileOtpRequestId) { this.mobileOtpRequestId = mobileOtpRequestId; }

    public String getEmailOtpRequestId() { return emailOtpRequestId; }
    public void setEmailOtpRequestId(String emailOtpRequestId) { this.emailOtpRequestId = emailOtpRequestId; }

    public String getPassportImageBase64() { return passportImageBase64; }
    public void setPassportImageBase64(String passportImageBase64) { this.passportImageBase64 = passportImageBase64; }

    public String getFaciaPassportReferenceId() { return faciaPassportReferenceId; }
    public void setFaciaPassportReferenceId(String faciaPassportReferenceId) { this.faciaPassportReferenceId = faciaPassportReferenceId; }

    public Map<String, Object> getExtractedData() { return extractedData; }
    public void setExtractedData(Map<String, Object> extractedData) { this.extractedData = extractedData; }

    public Map<String, Object> getConfirmedData() { return confirmedData; }
    public void setConfirmedData(Map<String, Object> confirmedData) { this.confirmedData = confirmedData; }

    public String getPassportIssueDate() { return passportIssueDate; }
    public void setPassportIssueDate(String passportIssueDate) { this.passportIssueDate = passportIssueDate; }

    public String getPassportExpiryDate() { return passportExpiryDate; }
    public void setPassportExpiryDate(String passportExpiryDate) { this.passportExpiryDate = passportExpiryDate; }

    public String getFaciaFaceMatchReferenceId() { return faciaFaceMatchReferenceId; }
    public void setFaciaFaceMatchReferenceId(String faciaFaceMatchReferenceId) { this.faciaFaceMatchReferenceId = faciaFaceMatchReferenceId; }

    public String getSullisSessionId() { return sullisSessionId; }
    public void setSullisSessionId(String sullisSessionId) { this.sullisSessionId = sullisSessionId; }

    public String getSullisAttemptId() { return sullisAttemptId; }
    public void setSullisAttemptId(String sullisAttemptId) { this.sullisAttemptId = sullisAttemptId; }

    public Double getFaceMatchScore() { return faceMatchScore; }
    public void setFaceMatchScore(Double faceMatchScore) { this.faceMatchScore = faceMatchScore; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(String keycloakUserId) { this.keycloakUserId = keycloakUserId; }

    public String getGlobalUid() { return globalUid; }
    public void setGlobalUid(String globalUid) { this.globalUid = globalUid; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getTokenExpiresIn() { return tokenExpiresIn; }
    public void setTokenExpiresIn(long tokenExpiresIn) { this.tokenExpiresIn = tokenExpiresIn; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public boolean isBiometricsEnabled() { return biometricsEnabled; }
    public void setBiometricsEnabled(boolean biometricsEnabled) { this.biometricsEnabled = biometricsEnabled; }

    public boolean isBiometricsSkipped() { return biometricsSkipped; }
    public void setBiometricsSkipped(boolean biometricsSkipped) { this.biometricsSkipped = biometricsSkipped; }

    public boolean isPinSet() { return pinSet; }
    public void setPinSet(boolean pinSet) { this.pinSet = pinSet; }

    public boolean isOnboardingComplete() { return onboardingComplete; }
    public void setOnboardingComplete(boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
