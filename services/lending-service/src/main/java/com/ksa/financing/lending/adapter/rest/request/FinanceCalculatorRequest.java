package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request for BRD UC#01 — Finance Calculator")
public record FinanceCalculatorRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        @Schema(description = "Financing amount in SAR (e.g., 1000, 1500, 2000)")
        BigDecimal amount,

        @Positive(message = "Tenure must be positive")
        @Schema(description = "Duration in months (default 3)", defaultValue = "3")
        int tenureMonths,

        @NotNull(message = "Total income is required")
        @Positive(message = "Total income must be positive")
        @Schema(description = "Total monthly income / salary in SAR")
        BigDecimal totalIncome,

        @Schema(description = "Product UUID (optional — uses default product if not provided)")
        String productId
) {}
