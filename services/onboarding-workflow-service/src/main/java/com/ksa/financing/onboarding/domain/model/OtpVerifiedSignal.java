package com.ksa.financing.onboarding.domain.model;

public record OtpVerifiedSignal(
    String nationalId,
    String keycloakUserId,
    DeviceInfo deviceInfo
) {}
