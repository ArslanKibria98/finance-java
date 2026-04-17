package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.model.RiskGrade;
import java.util.UUID;

public interface UpdateCustomerUseCase {
    Customer updateKycStatus(UUID tenantId, UUID customerId, KycStatus status);
    Customer updateLifecycleStage(UUID tenantId, UUID customerId, LifecycleStage stage);
    Customer updateRiskGrade(UUID tenantId, UUID customerId, RiskGrade grade);
    Customer updatePepFlag(UUID tenantId, UUID customerId, boolean pepFlag);
    Customer update(UUID tenantId, UUID customerId, UpdateCustomerCommand command);

    record UpdateCustomerCommand(
        String firstName,
        String lastName,
        String firstNameAr,
        String lastNameAr,
        String email,
        String mobileNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String profilePicture
    ) {}
}
