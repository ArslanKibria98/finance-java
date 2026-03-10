package com.ksa.financing.piivault.domain.port.in;

import com.ksa.financing.piivault.domain.model.PiiIndividual;
import java.util.UUID;

public interface StorePiiUseCase {
    PiiIndividual store(StorePiiCommand command);

    record StorePiiCommand(
        UUID globalUid,
        String nationalId,
        String nationalIdType,
        String fullName,
        String firstName,
        String middleName,
        String lastName,
        String fullNameAr,
        String dateOfBirth,
        String gender,
        String nationalityCode,
        String mobile,
        String email,
        String countryCode
    ) {}
}
