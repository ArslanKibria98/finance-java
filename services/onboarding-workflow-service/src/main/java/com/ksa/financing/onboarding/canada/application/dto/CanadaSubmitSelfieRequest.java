package com.ksa.financing.onboarding.canada.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CanadaSubmitSelfieRequest(
        @NotBlank @Email String email,
        @NotBlank String selfieImageBase64
) {
}
