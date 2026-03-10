package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record GosiSalaryRequest(
    @NotBlank String nationalId
) {}
