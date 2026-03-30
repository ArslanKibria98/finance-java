package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Step 1: Submit basic information for loan application")
public record SubmitBasicInfoRequest(

        @NotBlank(message = "Product ID is required")
        String productId,

        @NotNull(message = "Requested amount is required")
        @DecimalMin(value = "0.01", message = "Requested amount must be positive")
        BigDecimal requestedAmount,

        @Min(value = 1, message = "Tenure must be at least 1 month")
        int requestedTenureMonths,

        @Schema(description = "Purpose of finance")
        String purposeOfFinance,

        @Schema(description = "Partner ID (optional)")
        String partnerId,

        @Schema(description = "Lead ID (optional)")
        String leadId
) {}
