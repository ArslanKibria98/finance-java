package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record InitiateOnboardingRequest(
    @NotBlank String nationalId,
    @NotBlank String mobileNumber,
    @NotBlank String deviceId,
    String latitude,
    String longitude,
    String ipAddress,
    String userAgent
) {}
