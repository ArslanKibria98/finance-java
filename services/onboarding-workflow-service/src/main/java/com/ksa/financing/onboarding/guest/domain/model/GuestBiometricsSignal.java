package com.ksa.financing.onboarding.guest.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record GuestBiometricsSignal(
        boolean enabled,
        boolean skipped,
        DeviceInfo deviceInfo
) implements Serializable {
}
