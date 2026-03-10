package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(@NotBlank String nationalId, @NotBlank String mobileNumber, @NotBlank String otpRequestId) {}
