package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcceptTermsRequest(
    @NotBlank String nationalId,
    @NotNull Boolean accepted
) {}
