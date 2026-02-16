package com.ksa.financing.lms.exception;

/**
 * Exception for Fineract connection/network failures.
 */
public class FineractConnectionException extends FineractException {

    public FineractConnectionException(String message) {
        super(message);
    }

    public FineractConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}