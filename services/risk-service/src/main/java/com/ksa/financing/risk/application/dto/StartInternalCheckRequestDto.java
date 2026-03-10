package com.ksa.financing.risk.application.dto;

import jakarta.validation.constraints.NotBlank;

public record StartInternalCheckRequestDto(
    @NotBlank(message = "National ID is required")
    String nationalId,

    @NotBlank(message = "Mobile number is required")
    String mobileNumber
) {}
