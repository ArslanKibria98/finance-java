package com.ksa.financing.customer.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed PEP/EDD answers captured from the customer.
 * Same shape is used for onboarding-time and post-login flows.
 */
public record CustomerPepAnswer(
    UUID id,
    UUID tenantId,
    UUID customerId,
    boolean isPep,
    String politicalPosition,
    String governmentBody,
    String countryOfInfluence,
    String positionStartDate,
    String positionEndDate,
    String primarySourceOfWealth,
    String estimatedNetWorth,
    String sourceOfWealthDescription,
    String sourceOfFunds,
    String sourceOfFundsDetails,
    String occupation,
    String occupationDetails,
    List<RelatedPerson> relatedPersons,
    String additionalNotes,
    SubmittedVia submittedVia,
    Instant submittedAt,
    UUID submittedBy
) {

    public enum SubmittedVia { ONBOARDING, POST_LOGIN }

    public record RelatedPerson(String name, String relationship, String position) {}
}
