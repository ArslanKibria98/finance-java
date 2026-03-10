package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record StartOnboardingRequest(
    @NotBlank String nationalId,
    @NotBlank String mobileNumber
) {}
