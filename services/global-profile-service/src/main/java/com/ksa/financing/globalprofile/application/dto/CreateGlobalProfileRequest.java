package com.ksa.financing.globalprofile.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGlobalProfileRequest(
    String email,
    @NotBlank String mobile,
    @NotBlank String primaryCountryCode
) {}
