package com.ksa.financing.onboarding.canada.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record CanadaOnboardingRequest(
        String email,
        String mobileNumber,
        String tenantId,
        DeviceInfo deviceInfo
) implements Serializable {
}
