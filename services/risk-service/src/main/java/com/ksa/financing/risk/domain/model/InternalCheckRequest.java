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
    String tenantId,
    String countryCode
) {
    public boolean hasNationalId() {
        return nationalId != null && !nationalId.isBlank();
    }

    public boolean hasMobileNumber() {
        return mobileNumber != null && !mobileNumber.isBlank();
    }
}
