package com.ksa.financing.onboarding.guest.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record GuestPinSignal(
        String pin,
        String confirmPin,
        DeviceInfo deviceInfo
) implements Serializable {
}
