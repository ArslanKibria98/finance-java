package com.ksa.financing.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer login request using mobile number and 6-digit PIN")
public record LoginWithMobilePinRequest(
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+\\d{10,15}$", message = "Mobile number must be in international format (e.g., +966501234567)")
    @Schema(description = "Mobile number in international format", example = "+966501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    String mobileNumber,

    @NotBlank(message = "PIN is required")
    @Size(min = 6, max = 6, message = "PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
    @Schema(description = "6-digit app PIN set during onboarding", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    String pin
) {}
