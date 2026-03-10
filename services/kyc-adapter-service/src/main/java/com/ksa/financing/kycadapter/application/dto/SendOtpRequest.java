package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(@NotBlank String nationalId, @NotBlank String mobileNumber) {}
