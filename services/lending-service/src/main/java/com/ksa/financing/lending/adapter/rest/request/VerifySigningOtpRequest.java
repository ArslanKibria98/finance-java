package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Step 5b: Verify OTP for contract signing")
public record VerifySigningOtpRequest(

        @NotBlank(message = "OTP code is required")
        @Schema(description = "6-digit OTP code")
        String otpCode
) {}
