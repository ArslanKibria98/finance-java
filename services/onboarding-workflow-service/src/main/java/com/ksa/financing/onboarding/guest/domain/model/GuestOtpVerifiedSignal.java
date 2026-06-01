package com.ksa.financing.onboarding.guest.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record GuestOtpVerifiedSignal(
        String mobileOtpCode,
        String emailOtpCode,
        DeviceInfo deviceInfo
) implements Serializable {
}
