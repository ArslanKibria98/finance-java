package com.ksa.financing.onboarding.domain.model;

public record TermsAcceptedSignal(
    boolean accepted,
    DeviceInfo deviceInfo
) {}
