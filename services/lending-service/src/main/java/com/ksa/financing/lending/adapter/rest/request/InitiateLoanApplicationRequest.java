package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Request to initiate a new loan application or check eligibility")
public record InitiateLoanApplicationRequest(

        @NotBlank(message = "Mode is required (APPLY or ELIGIBILITY_CHECK)")
        @Schema(description = "APPLY = full application with amount, ELIGIBILITY_CHECK = just check eligibility",
                allowableValues = {"APPLY", "ELIGIBILITY_CHECK"})
        String mode,

        @NotBlank(message = "Customer ID is required")
        @Schema(description = "Customer UUID")
        String customerId,

        @NotBlank(message = "National ID is required")
        @Schema(description = "Saudi National ID")
        String nationalId,

        @NotBlank(message = "Mobile number is required")
        @Schema(description = "Mobile number (05xxxxxxxx)")
        String mobileNumber,

        @NotBlank(message = "Product ID is required")
        @Schema(description = "Selected product UUID")
        String productId,

        @Schema(description = "Requested financing amount in SAR (required for APPLY mode)")
        BigDecimal requestedAmount,

        @Schema(description = "Requested tenure in months (required for APPLY mode)")
        int requestedTenureMonths,

        @Schema(description = "Purpose of finance code (required for APPLY mode)")
        String purposeOfFinance,

        @Schema(description = "Purpose of finance other (when purposeOfFinance = OTHER)")
        String purposeOfFinanceOther,

        // Individual expense categories (BRD Section 3.3)
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

        @NotNull(message = "Salary is required for eligibility check")
        @Schema(description = "Monthly salary / total income in SAR (used for eligibility check)")
        BigDecimal salary,

        @Schema(description = "Total existing liabilities / obligations in SAR (used for eligibility check)")
        BigDecimal liabilities,

        @Schema(description = "Number of additional adults in household (default 0)")
        int additionalAdults,

        @Schema(description = "Number of children in household (default 0)")
        int numberOfChildren,

        @NotNull(message = "Eligibility answers are required")
        @Schema(description = "Dynamic eligibility field answers: field_key → value (e.g., monthly_income → 15000)")
        Map<String, String> eligibilityAnswers
) {}
