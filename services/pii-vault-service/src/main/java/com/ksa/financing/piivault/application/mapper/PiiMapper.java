package com.ksa.financing.piivault.application.mapper;

import com.ksa.financing.piivault.application.dto.PiiResponse;
import com.ksa.financing.piivault.domain.model.PiiIndividual;

public class PiiMapper {

    private PiiMapper() {}

    public static PiiResponse toResponse(PiiIndividual pii) {
        return new PiiResponse(
            pii.getPiiId(),
            pii.getGlobalUid(),
            pii.getNationalId(),
            pii.getNationalIdType(),
            pii.getFullName(),
            pii.getFirstName(),
            pii.getLastName(),
            pii.getFullNameAr(),
            pii.getDateOfBirth(),
            pii.getGender(),
            pii.getNationalityCode(),
            pii.getMobile(),
            pii.getEmail(),
            pii.getCountryCode(),
            pii.getCreatedAt()
        );
    }
}
