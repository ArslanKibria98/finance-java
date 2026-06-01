package com.ksa.financing.onboarding.foreign.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForeignInitiateRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "mobileNumber must be 7-15 digits, optional + prefix")
        String mobileNumber,
        // Optional at initiate — passport OCR fills countryOfOrigin, user confirms both at /confirm-data.
        @Pattern(regexp = "^([A-Z]{2,3})?$", message = "countryOfOrigin must be an ISO-3166 alpha-2 or alpha-3 code")
        String countryOfOrigin,
        @Pattern(regexp = "^([A-Z]{2,3})?$", message = "residentialCountry must be an ISO-3166 alpha-2 or alpha-3 code")
        String residentialCountry
) {
}
