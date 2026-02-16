package com.demo.islamic.compliance.exception;

/**
 * Exception for data residency violations
 */
public class DataResidencyViolationException extends ComplianceViolationException {

    private String dataLocation;
    private String requiredLocation;
    private String dataType;

    public DataResidencyViolationException(String message) {
        super(message);
        setViolationType("DATA_RESIDENCY");
        setRegulatoryBody("SAMA");
        setSeverity("CRITICAL");
    }

    public DataResidencyViolationException(String message, String dataLocation, String requiredLocation) {
        super(message);
        this.dataLocation = dataLocation;
        this.requiredLocation = requiredLocation;
        setViolationType("DATA_RESIDENCY");
        setRegulatoryBody("SAMA");
        setSeverity("CRITICAL");
    }

    public DataResidencyViolationException(String message, Throwable cause) {
        super(message, cause);
        setViolationType("DATA_RESIDENCY");
        setRegulatoryBody("SAMA");
        setSeverity("CRITICAL");
    }

    public String getDataLocation() {
        return dataLocation;
    }

    public void setDataLocation(String dataLocation) {
        this.dataLocation = dataLocation;
    }

    public String getRequiredLocation() {
        return requiredLocation;
    }

    public void setRequiredLocation(String requiredLocation) {
        this.requiredLocation = requiredLocation;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataResidencyViolationException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (dataLocation != null) {
            sb.append(", dataLocation='").append(dataLocation).append('\'');
        }
        if (requiredLocation != null) {
            sb.append(", requiredLocation='").append(requiredLocation).append('\'');
        }
        if (dataType != null) {
            sb.append(", dataType='").append(dataType).append('\'');
        }
        sb.append(", violationType='").append(getViolationType()).append('\'');
        sb.append(", regulatoryBody='").append(getRegulatoryBody()).append('\'');
        sb.append(", severity='").append(getSeverity()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}