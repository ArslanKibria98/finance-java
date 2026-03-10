package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record NafathInitiateRequest(
    @NotBlank String nationalId
) {}
