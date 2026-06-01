package com.ksa.financing.onboarding.canada.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CanadaBiometricsRequest(
        @NotBlank @Email String email,
        boolean enabled,
        boolean skipped
) {
}
