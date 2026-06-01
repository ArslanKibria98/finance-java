package com.ksa.financing.onboarding.guest.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record GuestOnboardingRequest(
        String email,
        String mobileNumber,
        String tenantId,
        DeviceInfo deviceInfo
) implements Serializable {
}
