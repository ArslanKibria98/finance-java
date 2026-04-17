package com.ksa.financing.onboarding.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnboardingRequest(
    String nationalId,
    String mobileNumber,
    String tenantId,
    String deviceId,
    String latitude,
    String longitude,
    String dateOfBirth,
    String countryCode
) {}
