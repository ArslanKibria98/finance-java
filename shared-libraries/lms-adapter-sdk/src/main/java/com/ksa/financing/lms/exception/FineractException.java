package com.ksa.financing.lms.exception;

/**
 * Base exception for Fineract-related errors.
 */
public class FineractException extends RuntimeException {

    private String errorCode;
    private Integer httpStatus;

    public FineractException(String message) {
        super(message);
    }

    public FineractException(String message, Throwable cause) {
        super(message, cause);
    }

    public FineractException(String message, String errorCode, Integer httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}