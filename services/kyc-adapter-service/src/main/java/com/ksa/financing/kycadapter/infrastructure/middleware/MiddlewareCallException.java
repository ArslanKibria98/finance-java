package com.ksa.financing.kycadapter.infrastructure.middleware;

public class MiddlewareCallException extends RuntimeException {

    private final String apiCode;

    public MiddlewareCallException(String apiCode, Throwable cause) {
        super("Middleware call failed for apiCode=" + apiCode + ": " + cause.getMessage(), cause);
        this.apiCode = apiCode;
    }

    public String getApiCode() {
        return apiCode;
    }
}
