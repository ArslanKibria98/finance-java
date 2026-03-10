package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record InitiateNafathRequest(
    @NotBlank String nationalId
) {}
