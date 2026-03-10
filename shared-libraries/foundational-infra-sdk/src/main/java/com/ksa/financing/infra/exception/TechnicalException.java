package com.ksa.financing.infra.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public TechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public TechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }

    public TechnicalException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}
