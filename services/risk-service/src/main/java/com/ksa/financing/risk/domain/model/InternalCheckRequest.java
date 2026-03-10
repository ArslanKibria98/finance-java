package com.ksa.financing.risk.domain.model;

public record InternalCheckRequest(
    String nationalId,
    String nidHash,
    String mobileNumber,
    String mobileHash,
    String deviceId,
    String deviceFingerprint,
    String ipAddress,
    String sessionId,
    String tenantId
) {}
