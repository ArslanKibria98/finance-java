package com.demo.islamic.compliance.exception;

/**
 * Exception for ZATCA API related errors
 */
public class ZatcaException extends RuntimeException {

    private String errorCode;
    private String zatcaMessage;
    private int httpStatusCode;

    public ZatcaException(String message) {
        super(message);
    }

    public ZatcaException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZatcaException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ZatcaException(String message, String errorCode, String zatcaMessage, int httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.zatcaMessage = zatcaMessage;
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getZatcaMessage() {
        return zatcaMessage;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ZatcaException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (errorCode != null) {
            sb.append(", errorCode='").append(errorCode).append('\'');
        }
        if (zatcaMessage != null) {
            sb.append(", zatcaMessage='").append(zatcaMessage).append('\'');
        }
        if (httpStatusCode > 0) {
            sb.append(", httpStatusCode=").append(httpStatusCode);
        }
        sb.append('}');
        return sb.toString();
    }
}