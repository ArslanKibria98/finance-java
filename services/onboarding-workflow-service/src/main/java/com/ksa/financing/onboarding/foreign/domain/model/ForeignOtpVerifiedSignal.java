package com.ksa.financing.onboarding.foreign.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record ForeignOtpVerifiedSignal(
        String mobileOtpCode,
        String emailOtpCode,
        DeviceInfo deviceInfo
) implements Serializable {
}
