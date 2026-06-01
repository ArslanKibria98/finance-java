package com.ksa.financing.onboarding.guest.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestSetPinRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "pin must be 6 digits") String pin,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "confirmPin must be 6 digits") String confirmPin
) {
}
