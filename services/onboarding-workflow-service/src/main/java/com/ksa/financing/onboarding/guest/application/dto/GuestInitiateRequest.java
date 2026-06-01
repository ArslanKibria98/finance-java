package com.ksa.financing.onboarding.guest.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestInitiateRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "mobileNumber must be 7-15 digits, optional + prefix")
        String mobileNumber
) {
}
