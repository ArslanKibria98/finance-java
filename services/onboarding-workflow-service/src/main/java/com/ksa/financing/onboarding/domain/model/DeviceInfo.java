package com.ksa.financing.onboarding.domain.model;

public record DeviceInfo(
    String deviceId,
    String latitude,
    String longitude,
    String ipAddress,
    String userAgent
) {}
