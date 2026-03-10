package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(@NotBlank String nationalId, @NotBlank String otpCode, @NotBlank String otpRequestId) {}
