package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
    @NotBlank String nationalId,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "PIN must be exactly 6 digits") String pin,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "Confirm PIN must be exactly 6 digits") String confirmPin
) {}
