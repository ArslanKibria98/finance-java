package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record StartOnboardingRequest(
    @NotBlank String nationalId,
    @NotBlank String mobileNumber,
    @com.fasterxml.jackson.annotation.JsonAlias({"resolvedCountryCode", "country_code"})
    String countryCode
) {
    public String resolvedCountryCode() {
        return countryCode != null && !countryCode.isBlank() ? countryCode.toUpperCase() : "SAU";
    }
}
