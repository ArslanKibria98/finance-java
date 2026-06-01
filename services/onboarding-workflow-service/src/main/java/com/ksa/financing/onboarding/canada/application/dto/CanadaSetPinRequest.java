package com.ksa.financing.onboarding.canada.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CanadaSetPinRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "pin must be 6 digits") String pin,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "confirmPin must be 6 digits") String confirmPin
) {
}
