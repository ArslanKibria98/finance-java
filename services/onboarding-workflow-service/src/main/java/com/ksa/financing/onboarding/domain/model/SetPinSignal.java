package com.ksa.financing.onboarding.domain.model;

public record SetPinSignal(
    String deviceId,
    String pin,
    String confirmPin
) {}
