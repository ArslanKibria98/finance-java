package com.ksa.financing.kycadapter.application.dto;

import java.util.Map;
import java.util.UUID;

public record YakeenVerifyResponse(
    UUID sessionId,
    String result,
    Map<String, Object> demographics
) {}
