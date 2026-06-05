package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.Customer;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateCustomerUseCase {
    Customer create(CreateCustomerCommand command);

    record CreateCustomerCommand(
        UUID tenantId,
        String nationalId,
        String nationalIdType,
        String firstName,
        String middleName,
        String lastName,
        String firstNameAr,
        String lastNameAr,
        LocalDate dateOfBirth,
        String gender,
        String nationality,
        String residencyType,
        String mobileNumber,
        String email,
        UUID keycloakUserId,
        String lifecycleStage,
        UUID globalUid,
        String idempotencyKey,
        String onboardingFlow
    ) {}
}
