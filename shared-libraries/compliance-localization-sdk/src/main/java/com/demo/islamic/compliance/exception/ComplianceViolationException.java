package com.demo.islamic.compliance.exception;

/**
 * Exception for regulatory compliance violations
 */
public class ComplianceViolationException extends RuntimeException {

    private String violationType;
    private String regulatoryBody;
    private String severity;
    private String remediationRequired;

    public ComplianceViolationException(String message) {
        super(message);
    }

    public ComplianceViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComplianceViolationException(String message, String violationType, String regulatoryBody) {
        super(message);
        this.violationType = violationType;
        this.regulatoryBody = regulatoryBody;
    }

    public ComplianceViolationException(String message, String violationType,
                                       String regulatoryBody, String severity) {
        super(message);
        this.violationType = violationType;
        this.regulatoryBody = regulatoryBody;
        this.severity = severity;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getRegulatoryBody() {
        return regulatoryBody;
    }

    public void setRegulatoryBody(String regulatoryBody) {
        this.regulatoryBody = regulatoryBody;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRemediationRequired() {
        return remediationRequired;
    }

    public void setRemediationRequired(String remediationRequired) {
        this.remediationRequired = remediationRequired;
    }

    /**
     * Check if this is a critical violation requiring immediate action
     */
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(severity) || "HIGH".equalsIgnoreCase(severity);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComplianceViolationException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (violationType != null) {
            sb.append(", violationType='").append(violationType).append('\'');
        }
        if (regulatoryBody != null) {
            sb.append(", regulatoryBody='").append(regulatoryBody).append('\'');
        }
        if (severity != null) {
            sb.append(", severity='").append(severity).append('\'');
        }
        if (remediationRequired != null) {
            sb.append(", remediationRequired='").append(remediationRequired).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}