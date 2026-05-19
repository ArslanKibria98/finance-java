package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Enhanced Due Diligence (EDD) form request.
 * Required when PEP screening detects a politically exposed person (>= 60% confidence).
 *
 * Sections:
 *   1. Political Position details
 *   2. Source of Wealth
 *   3. Source of Funds
 *   4. Occupation (LOV code from customer-service)
 *   5. Related Persons (family in political positions)
 *   6. Supporting Documents (handled via separate file upload endpoint)
 */
public record SubmitEddRequest(

    @NotBlank String nationalId,

    // --- Political Position ---
    @NotBlank(message = "Political position is required")
    String politicalPosition,       // Head of State, Minister, Parliamentarian, Senior Military,
                                    // Judge, Central Bank Governor, Ambassador,
                                    // Board of State Enterprise, Senior Political Party Official, Other

    @NotBlank(message = "Government body is required")
    @Size(min = 3, max = 200, message = "Government body must be between 3 and 200 characters")
    String governmentBody,

    @NotBlank(message = "Country of influence is required")
    String countryOfInfluence,      // ISO 3166 country code

    @NotBlank(message = "Position start date is required")
    String positionStartDate,       // DD/MM/YYYY — cannot be future date

    String positionEndDate,         // DD/MM/YYYY — must be after start date if provided (null = current)

    // --- Source of Wealth ---
    @NotBlank(message = "Primary source of wealth is required")
    String primarySourceOfWealth,   // Salary, Business Ownership, Inheritance, Investments, Real Estate, Other

    @NotBlank(message = "Estimated net worth is required")
    String estimatedNetWorth,       // < 1M, 1M-5M, 5M-10M, 10M-50M, > 50M SAR

    @NotBlank(message = "Source of wealth description is required")
    @Size(min = 50, message = "Source of wealth description must be at least 50 characters")
    String sourceOfWealthDescription,

    // --- Source of Funds ---
    @NotBlank(message = "Source of funds for account is required")
    String sourceOfFunds,           // Salary, Business Profits, Asset Sale, Inheritance, Savings, Other

    @NotBlank(message = "Source of funds details is required")
    @Size(min = 30, message = "Source of funds details must be at least 30 characters")
    String sourceOfFundsDetails,

    /** Occupation LOV code from {@code GET /api/v1/reference-data/occupation/active} (e.g. {@code EMPLOYED_PUBLIC}). */
    @NotBlank(message = "Occupation is required")
    @Size(max = 50, message = "Occupation code must not exceed 50 characters")
    String occupation,

    // --- Related Persons (optional) ---
    @Valid
    List<RelatedPerson> relatedPersons,

    // --- Additional Notes ---
    String additionalNotes

) {
    /**
     * Related person record — family members in political positions.
     */
    public record RelatedPerson(
        @NotBlank(message = "Related person name is required")
        String name,

        @NotBlank(message = "Relationship is required")
        String relationship,

        String position                // Political position if applicable
    ) {}
}
