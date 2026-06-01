package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.CustomerPepAnswer;

import java.util.List;
import java.util.UUID;

public interface SubmitPepAnswerUseCase {

    CustomerPepAnswer submit(UUID tenantId, UUID customerId, SubmitPepAnswerCommand command);

    record SubmitPepAnswerCommand(
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
            List<CustomerPepAnswer.RelatedPerson> relatedPersons,
            String additionalNotes,
            UUID submittedBy
    ) {}
}
