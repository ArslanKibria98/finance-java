package com.ksa.financing.infra.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public NotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public NotFoundException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public static NotFoundException forEntity(String entityType, String identifier) {
        return new NotFoundException(
                ErrorCodes.NOT_FOUND,
                String.format("%s not found with identifier: %s", entityType, identifier),
                entityType, identifier);
    }
}
