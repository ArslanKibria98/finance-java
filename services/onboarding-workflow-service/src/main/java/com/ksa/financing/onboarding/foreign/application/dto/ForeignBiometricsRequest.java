package com.ksa.financing.onboarding.foreign.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForeignBiometricsRequest(
        @NotBlank @Email String email,
        boolean enabled,
        boolean skipped
) {
}
