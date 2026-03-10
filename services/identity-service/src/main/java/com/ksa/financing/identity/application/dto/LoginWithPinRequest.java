package com.ksa.financing.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer login request using National ID and 6-digit PIN")
public record LoginWithPinRequest(
    @NotBlank(message = "National ID is required")
    @Size(min = 10, max = 10, message = "National ID must be 10 digits")
    @Pattern(regexp = "^[12]\\d{9}$", message = "National ID must start with 1 or 2 followed by 9 digits")
    @Schema(description = "Saudi National ID (10 digits, starts with 1 or 2)", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    String nationalId,

    @NotBlank(message = "PIN is required")
    @Size(min = 6, max = 6, message = "PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
    @Schema(description = "6-digit app PIN set during onboarding", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    String pin
) {}
