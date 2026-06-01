package com.ksa.financing.infra.exception;

import lombok.Getter;

/**
 * Thrown to signal a resource conflict (HTTP 409). Mapped by
 * {@link GlobalExceptionHandler} to a 409 response that preserves the
 * caller-supplied {@code errorCode} (e.g. {@code ONBOARDING.EMAIL.ALREADY_REGISTERED}),
 * so the client can branch on a stable machine-readable code rather than
 * the catch-all {@code COMMON.RESOURCE.CONFLICT}.
 */
@Getter
public class ConflictException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public ConflictException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
}
