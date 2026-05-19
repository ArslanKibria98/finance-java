package com.ksa.financing.onboarding.domain.model;

import java.util.List;

/**
 * Signal carrying Enhanced Due Diligence form data from the client.
 * Sent when workflow is in EDD_REQUIRED state (PEP detected >= 60% confidence).
 */
public record EddFormSignal(
    String deviceId,

    // Political Position
    String politicalPosition,
    String governmentBody,
    String countryOfInfluence,
    String positionStartDate,
    String positionEndDate,

    // Source of Wealth
    String primarySourceOfWealth,
    String estimatedNetWorth,
    String sourceOfWealthDescription,

    // Source of Funds
    String sourceOfFunds,
    String sourceOfFundsDetails,

    /** Occupation LOV code (nullable for replay of older workflow signal payloads). */
    String occupation,

    // Related Persons
    List<RelatedPerson> relatedPersons,

    // Additional Notes
    String additionalNotes
) {
    public record RelatedPerson(
        String name,
        String relationship,
        String position
    ) {}
}
