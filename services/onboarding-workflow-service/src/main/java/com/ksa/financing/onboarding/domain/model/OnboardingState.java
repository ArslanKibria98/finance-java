package com.ksa.financing.onboarding.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OnboardingState implements Serializable {

    private static final long serialVersionUID = 1L;

    private OnboardingStep currentStep;
    private String workflowId;
    private String nationalId;
    private String mobileNumber;
    private String globalUid;
    private String customerId;
    private String walletId;
    private String keycloakUserId;
    private String otpRequestId;
    private int nafathRandomNumber;
    private String nafathSessionId;
    private String nafathTransactionId;
    private Map<String, Object> nafathVerificationData;
    private String initialDeviceId;
    private boolean deviceTrusted;
    private String failureReason;
    private Map<String, Object> yakeenData;
    private String lifecycleStage;
    private String pepDecision;         // CLEAR, FLAG, HOLD, BLOCK, EDD_REQUIRED
    private double pepConfidence;
    private boolean pepDetected;
    private String riskLevel;           // LOW, MEDIUM, HIGH, CRITICAL
    private int riskScore;
    private String riskDecision;        // APPROVE, APPROVE_FLAG, HOLD, BLOCK
    private String eddStatus;           // null, PENDING, SUBMITTED, APPROVED
    private String eddPoliticalPosition;
    private String eddGovernmentBody;
    private String eddCountryOfInfluence;
    private String eddSourceOfWealth;
    private String eddEstimatedNetWorth;
    private String eddSourceOfFunds;
    private String amlAssessmentId;     // UUID from risk-service AML scoring
    private String amlRiskLevel;        // HIGH, MEDIUM, LOW from AML scoring
    private double amlTotalScore;       // weighted AML risk score
    private boolean amlDominantOverride;// true if PEP/Internal List triggers auto-HIGH
    private String amlDominantCategory; // PEP or INTERNAL_LIST if dominant override
    private boolean pinSet;             // true when customer has set their app PIN
    private String countryCode;         // ISO 3166-1 alpha-3 (SAU, ARE, PAK)
    private Instant startedAt;
    private Instant lastUpdatedAt;

    public OnboardingState() {}

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    // --- Getters and Setters ---

    public OnboardingStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(OnboardingStep currentStep) {
        this.currentStep = currentStep;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getGlobalUid() {
        return globalUid;
    }

    public void setGlobalUid(String globalUid) {
        this.globalUid = globalUid;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public String getOtpRequestId() {
        return otpRequestId;
    }

    public void setOtpRequestId(String otpRequestId) {
        this.otpRequestId = otpRequestId;
    }

    public int getNafathRandomNumber() {
        return nafathRandomNumber;
    }

    public void setNafathRandomNumber(int nafathRandomNumber) {
        this.nafathRandomNumber = nafathRandomNumber;
    }

    public String getNafathSessionId() {
        return nafathSessionId;
    }

    public void setNafathSessionId(String nafathSessionId) {
        this.nafathSessionId = nafathSessionId;
    }

    public String getNafathTransactionId() {
        return nafathTransactionId;
    }

    public void setNafathTransactionId(String nafathTransactionId) {
        this.nafathTransactionId = nafathTransactionId;
    }

    public Map<String, Object> getNafathVerificationData() {
        return nafathVerificationData;
    }

    public void setNafathVerificationData(Map<String, Object> nafathVerificationData) {
        this.nafathVerificationData = nafathVerificationData;
    }

    public String getInitialDeviceId() {
        return initialDeviceId;
    }

    public void setInitialDeviceId(String initialDeviceId) {
        this.initialDeviceId = initialDeviceId;
    }

    public boolean isDeviceTrusted() {
        return deviceTrusted;
    }

    public void setDeviceTrusted(boolean deviceTrusted) {
        this.deviceTrusted = deviceTrusted;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Map<String, Object> getYakeenData() {
        return yakeenData;
    }

    public void setYakeenData(Map<String, Object> yakeenData) {
        this.yakeenData = yakeenData;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getLifecycleStage() {
        return lifecycleStage;
    }

    public void setLifecycleStage(String lifecycleStage) {
        this.lifecycleStage = lifecycleStage;
    }

    public String getPepDecision() {
        return pepDecision;
    }

    public void setPepDecision(String pepDecision) {
        this.pepDecision = pepDecision;
    }

    public double getPepConfidence() {
        return pepConfidence;
    }

    public void setPepConfidence(double pepConfidence) {
        this.pepConfidence = pepConfidence;
    }

    public boolean isPepDetected() {
        return pepDetected;
    }

    public void setPepDetected(boolean pepDetected) {
        this.pepDetected = pepDetected;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskDecision() {
        return riskDecision;
    }

    public void setRiskDecision(String riskDecision) {
        this.riskDecision = riskDecision;
    }

    public String getEddStatus() {
        return eddStatus;
    }

    public void setEddStatus(String eddStatus) {
        this.eddStatus = eddStatus;
    }

    public String getEddPoliticalPosition() {
        return eddPoliticalPosition;
    }

    public void setEddPoliticalPosition(String eddPoliticalPosition) {
        this.eddPoliticalPosition = eddPoliticalPosition;
    }

    public String getEddGovernmentBody() {
        return eddGovernmentBody;
    }

    public void setEddGovernmentBody(String eddGovernmentBody) {
        this.eddGovernmentBody = eddGovernmentBody;
    }

    public String getEddCountryOfInfluence() {
        return eddCountryOfInfluence;
    }

    public void setEddCountryOfInfluence(String eddCountryOfInfluence) {
        this.eddCountryOfInfluence = eddCountryOfInfluence;
    }

    public String getEddSourceOfWealth() {
        return eddSourceOfWealth;
    }

    public void setEddSourceOfWealth(String eddSourceOfWealth) {
        this.eddSourceOfWealth = eddSourceOfWealth;
    }

    public String getEddEstimatedNetWorth() {
        return eddEstimatedNetWorth;
    }

    public void setEddEstimatedNetWorth(String eddEstimatedNetWorth) {
        this.eddEstimatedNetWorth = eddEstimatedNetWorth;
    }

    public String getEddSourceOfFunds() {
        return eddSourceOfFunds;
    }

    public void setEddSourceOfFunds(String eddSourceOfFunds) {
        this.eddSourceOfFunds = eddSourceOfFunds;
    }

    public String getAmlAssessmentId() {
        return amlAssessmentId;
    }

    public void setAmlAssessmentId(String amlAssessmentId) {
        this.amlAssessmentId = amlAssessmentId;
    }

    public String getAmlRiskLevel() {
        return amlRiskLevel;
    }

    public void setAmlRiskLevel(String amlRiskLevel) {
        this.amlRiskLevel = amlRiskLevel;
    }

    public double getAmlTotalScore() {
        return amlTotalScore;
    }

    public void setAmlTotalScore(double amlTotalScore) {
        this.amlTotalScore = amlTotalScore;
    }

    public boolean isAmlDominantOverride() {
        return amlDominantOverride;
    }

    public void setAmlDominantOverride(boolean amlDominantOverride) {
        this.amlDominantOverride = amlDominantOverride;
    }

    public String getAmlDominantCategory() {
        return amlDominantCategory;
    }

    public void setAmlDominantCategory(String amlDominantCategory) {
        this.amlDominantCategory = amlDominantCategory;
    }

    public boolean isPinSet() {
        return pinSet;
    }

    public void setPinSet(boolean pinSet) {
        this.pinSet = pinSet;
    }
}
