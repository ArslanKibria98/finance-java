package com.ksa.financing.onboarding.foreign.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record ForeignPinSignal(
        String pin,
        String confirmPin,
        DeviceInfo deviceInfo
) implements Serializable {
}
