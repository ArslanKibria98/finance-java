package com.ksa.financing.onboarding.canada.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record DualOtpVerifiedSignal(
        String mobileOtpCode,
        String emailOtpCode,
        DeviceInfo deviceInfo
) implements Serializable {
}
