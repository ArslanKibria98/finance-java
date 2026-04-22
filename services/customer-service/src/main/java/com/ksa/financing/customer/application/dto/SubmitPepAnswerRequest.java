package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;

import java.util.List;

public record SubmitPepAnswerRequest(
        Boolean isPep,
        String politicalPosition,
        String governmentBody,
        String countryOfInfluence,
        String positionStartDate,
        String positionEndDate,
        @JsonAlias("sourceOfIncome")
        String primarySourceOfWealth,
        String estimatedNetWorth,
        String sourceOfWealthDescription,
        String sourceOfFunds,
        String sourceOfFundsDetails,
        @Valid List<RelatedPersonRequest> relatedPersons,
        String additionalNotes
) {
    public record RelatedPersonRequest(
            String name,
            String relationship,
            String position
    ) {}
}
