package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
    @NotBlank String nationalId,
    @NotBlank String mobileNumber,
    @NotBlank String otpCode
) {}
