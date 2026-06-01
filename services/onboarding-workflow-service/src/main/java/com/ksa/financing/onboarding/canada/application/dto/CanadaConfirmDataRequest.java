package com.ksa.financing.onboarding.canada.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CanadaConfirmDataRequest(
        @NotBlank @Email String email,
        @NotBlank String surname,
        @NotBlank String givenName,
        @NotBlank String nationality,
        @NotBlank String dateOfBirth,
        @NotBlank String documentNumber,
        // Optional — passport/ID OCR rarely returns address. Mobile may collect it
        // later or leave blank if not required upstream.
        String homeAddress
) {
}
