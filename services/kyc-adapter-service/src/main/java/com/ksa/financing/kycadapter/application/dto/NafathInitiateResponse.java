package com.ksa.financing.kycadapter.application.dto;

import java.util.Map;
import java.util.UUID;

public record NafathInitiateResponse(
    UUID sessionId,
    int randomNumber,
    String transactionId,
    String status,
    Map<String, Object> nafathVerificationData
) {}
