package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyMobileRequest(
    @NotBlank String mobileNumber,
    @NotBlank String nationalId
) {}
