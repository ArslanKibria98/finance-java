package com.ksa.financing.customer.domain.model;

import java.util.UUID;

/**
 * Domain model representing a single onboarding step configuration for a country.
 */
public class CountryOnboardingProfile {
    private UUID id;
    private UUID tenantId;
    private String countryCode;
    private int stepOrder;
    private String stepType;
    private String stepLabel;
    private String stepLabelAr;
    private String description;
    private String providerCode;
    private boolean signalWait;
    private int timeoutMinutes;
    private boolean required;
    private boolean enabled;
    private String configJson;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }
    public String getStepLabel() { return stepLabel; }
    public void setStepLabel(String stepLabel) { this.stepLabel = stepLabel; }
    public String getStepLabelAr() { return stepLabelAr; }
    public void setStepLabelAr(String stepLabelAr) { this.stepLabelAr = stepLabelAr; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public boolean isSignalWait() { return signalWait; }
    public void setSignalWait(boolean signalWait) { this.signalWait = signalWait; }
    public int getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}
