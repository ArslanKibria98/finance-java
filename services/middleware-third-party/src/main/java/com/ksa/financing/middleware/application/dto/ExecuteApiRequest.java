package com.ksa.financing.middleware.application.dto;

import java.util.Map;

public record ExecuteApiRequest(
        String secretKey,
        String requestBody,
        Map<String, String> pathParams,
        Map<String, String> queryParams,
        Map<String, String> headers,
        String idempotencyKey,
        String nationalId,
        String callerService
) {}
