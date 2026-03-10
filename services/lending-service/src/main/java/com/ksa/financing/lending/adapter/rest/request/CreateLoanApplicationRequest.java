package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to create a new loan application")
public record CreateLoanApplicationRequest(

        @NotNull(message = "Customer ID is required")
        @Schema(description = "Customer UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String customerId,

        @NotNull(message = "Product ID is required")
        @Schema(description = "Product UUID", example = "550e8400-e29b-41d4-a716-446655440001")
        String productId,

        @NotBlank(message = "Product code is required")
        @Schema(description = "Product code", example = "MURABAHA_PERSONAL")
        String productCode,

        @NotBlank(message = "Sharia structure is required")
        @Schema(description = "Sharia structure type", example = "MURABAHA")
        String shariaStructure,

        @NotNull(message = "Requested amount is required")
        @DecimalMin(value = "0.01", message = "Requested amount must be positive")
        @Schema(description = "Requested financing amount in SAR", example = "100000.00")
        BigDecimal requestedAmount,

        @Min(value = 1, message = "Tenure must be at least 1 month")
        @Schema(description = "Requested tenure in months", example = "60")
        int requestedTenureMonths,

        @Schema(description = "Partner UUID (optional)", example = "550e8400-e29b-41d4-a716-446655440002")
        String partnerId,

        @Schema(description = "Lead UUID (optional)", example = "550e8400-e29b-41d4-a716-446655440003")
        String leadId,

        @Schema(description = "Idempotency key to prevent duplicate applications (optional)", example = "app-req-12345")
        String idempotencyKey
) {}
