package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Financial-first product suggestion request — no productId, no amount, no tenure. System calculates max eligible amount per product from customer financial profile (SIMAH-backed liabilities + expenses).")
public record SuggestProductsRequest(

        @NotNull(message = "Salary is required")
        @Positive(message = "Salary must be positive")
        @Schema(description = "Monthly salary in SAR")
        BigDecimal salary,

        @Schema(description = "Existing monthly liabilities in SAR")
        BigDecimal liabilities,

        @Schema(description = "Number of adult dependents", defaultValue = "0")
        int adultDependents,

        @Schema(description = "Number of child dependents", defaultValue = "0")
        int childDependents,

        @Schema(description = "Monthly food & groceries expense in SAR")
        BigDecimal foodGroceries,

        @Schema(description = "Monthly utilities expense in SAR")
        BigDecimal utilities,

        @Schema(description = "Monthly healthcare expense in SAR")
        BigDecimal healthcare,

        @Schema(description = "Monthly communication expense in SAR")
        BigDecimal communication,

        @Schema(description = "Monthly housing/rent expense in SAR")
        BigDecimal housingRent,

        @Schema(description = "Monthly clothing & essentials expense in SAR")
        BigDecimal clothingEssentials,

        @Schema(description = "Monthly education expense in SAR")
        BigDecimal education,

        @Schema(description = "Monthly transportation expense in SAR")
        BigDecimal transportation
) {}
