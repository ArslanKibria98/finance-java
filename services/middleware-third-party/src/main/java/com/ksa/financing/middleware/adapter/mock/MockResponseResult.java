package com.ksa.financing.middleware.adapter.mock;

public record MockResponseResult(
        int httpStatus,
        String responseBody,
        String responseHeaders
) {}
