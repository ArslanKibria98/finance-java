package com.ksa.financing.lms.exception;

/**
 * Exception for idempotency-related issues.
 */
public class IdempotencyException extends RuntimeException {

    public IdempotencyException(String message) {
        super(message);
    }

    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}