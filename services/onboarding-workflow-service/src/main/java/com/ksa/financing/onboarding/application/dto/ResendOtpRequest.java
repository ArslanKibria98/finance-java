package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
    @NotBlank String nationalId,
    @NotBlank String mobileNumber
) {}
