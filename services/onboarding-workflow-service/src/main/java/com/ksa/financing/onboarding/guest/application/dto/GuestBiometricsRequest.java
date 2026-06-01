package com.ksa.financing.onboarding.guest.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GuestBiometricsRequest(
        @NotBlank @Email String email,
        boolean enabled,
        boolean skipped
) {
}
