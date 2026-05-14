package com.ksa.financing.infra.security.blacklist;

import lombok.Getter;

/**
 * Thrown by {@link BlacklistGuardFilter} when an inbound request matches a blacklist entry.
 * Mapped to HTTP 403 by GlobalExceptionHandler.
 */
@Getter
public class BlacklistViolationException extends RuntimeException {

    private final String errorCode;
    private final BlacklistType type;
    private final String reason;

    public BlacklistViolationException(BlacklistType type, String reason) {
        super("Request blocked by blacklist guard: type=" + type + ", reason=" + reason);
        this.type = type;
        this.errorCode = type.errorCode();
        this.reason = reason;
    }
}
