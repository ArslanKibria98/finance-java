package com.ksa.financing.onboarding.foreign.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForeignConfirmDataRequest(
        @NotBlank @Email String email,
        @NotBlank String surname,
        @NotBlank String givenName,
        @NotBlank String nationality,
        @NotBlank String dateOfBirth,
        // Mobile app sends `documentNumber` (shared name with Canada flow) — accept both.
        @NotBlank @JsonAlias("documentNumber") String passportNumber,
        // Optional — FACIA passport OCR rarely returns address; the mobile app can
        // collect it later in the journey (or leave blank if not required upstream).
        String homeAddress,
        // Optional — passport OCR / initiate already capture these; user may not edit them.
        String issueDate,
        String expiryDate,
        String countryOfOrigin,
        String residentialCountry
) {
}
