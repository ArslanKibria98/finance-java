package com.ksa.financing.middleware.application.dto;

public record ExecuteApiResponse(
        String requestId,
        int httpStatus,
        Object responseBody,
        long durationMs,
        boolean success,
        String errorMessage
) {}
