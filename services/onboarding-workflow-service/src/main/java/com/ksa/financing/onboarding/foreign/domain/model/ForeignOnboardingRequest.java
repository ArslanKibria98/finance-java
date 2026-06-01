package com.ksa.financing.onboarding.foreign.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record ForeignOnboardingRequest(
        String email,
        String mobileNumber,
        String countryOfOrigin,
        String residentialCountry,
        String tenantId,
        DeviceInfo deviceInfo
) implements Serializable {
}
