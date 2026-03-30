package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "BRD Steps 4-10: Pre-qualification / Check Eligibility request")
public record CheckEligibilityRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        @Schema(description = "Requested financing amount in SAR")
        BigDecimal amount,

        @Positive(message = "Tenure must be positive")
        @Schema(description = "Duration in months (default 3)", defaultValue = "3")
        int tenureMonths,

        @NotNull(message = "Salary is required")
        @Positive(message = "Salary must be positive")
        @Schema(description = "Monthly salary in SAR (minimum required: 4000)")
        BigDecimal salary,

        @Schema(description = "Existing monthly liabilities in SAR")
        BigDecimal liabilities,

        @Schema(description = "Number of adult dependents", defaultValue = "1")
        int adultDependents,

        @Schema(description = "Number of child dependents", defaultValue = "1")
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
        BigDecimal transportation,

        @Schema(description = "Product UUID (optional)")
        String productId
) {}
