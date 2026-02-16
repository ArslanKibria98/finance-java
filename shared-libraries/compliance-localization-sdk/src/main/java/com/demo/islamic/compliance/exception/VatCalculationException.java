package com.demo.islamic.compliance.exception;

import java.math.BigDecimal;

/**
 * Exception for VAT calculation errors
 */
public class VatCalculationException extends RuntimeException {

    private BigDecimal amount;
    private BigDecimal vatRate;
    private String calculationType;
    private String errorDetails;

    public VatCalculationException(String message) {
        super(message);
    }

    public VatCalculationException(String message, Throwable cause) {
        super(message, cause);
    }

    public VatCalculationException(String message, BigDecimal amount, BigDecimal vatRate) {
        super(message);
        this.amount = amount;
        this.vatRate = vatRate;
    }

    public VatCalculationException(String message, BigDecimal amount,
                                  BigDecimal vatRate, String calculationType) {
        super(message);
        this.amount = amount;
        this.vatRate = vatRate;
        this.calculationType = calculationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public String getCalculationType() {
        return calculationType;
    }

    public void setCalculationType(String calculationType) {
        this.calculationType = calculationType;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VatCalculationException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (amount != null) {
            sb.append(", amount=").append(amount);
        }
        if (vatRate != null) {
            sb.append(", vatRate=").append(vatRate);
        }
        if (calculationType != null) {
            sb.append(", calculationType='").append(calculationType).append('\'');
        }
        if (errorDetails != null) {
            sb.append(", errorDetails='").append(errorDetails).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}