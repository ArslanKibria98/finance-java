package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SanctionsScreenRequest(
    @NotBlank String fullName,
    @NotBlank String nationalId,
    @NotBlank String nationality
) {}
