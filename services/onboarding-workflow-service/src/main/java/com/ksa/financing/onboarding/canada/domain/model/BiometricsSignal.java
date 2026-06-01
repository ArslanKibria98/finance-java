package com.ksa.financing.onboarding.canada.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record BiometricsSignal(
        boolean enabled,
        boolean skipped,
        DeviceInfo deviceInfo
) implements Serializable {
}
