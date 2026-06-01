package com.ksa.financing.onboarding.foreign.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record ForeignPassportSubmittedSignal(
        String passportImageBase64,
        DeviceInfo deviceInfo
) implements Serializable {
}
