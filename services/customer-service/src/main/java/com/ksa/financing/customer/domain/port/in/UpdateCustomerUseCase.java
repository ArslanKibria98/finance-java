package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import java.util.UUID;

public interface UpdateCustomerUseCase {
    Customer updateKycStatus(UUID tenantId, UUID customerId, KycStatus status);
    Customer updateLifecycleStage(UUID tenantId, UUID customerId, LifecycleStage stage);
    Customer update(UUID tenantId, UUID customerId, UpdateCustomerCommand command);

    record UpdateCustomerCommand(
        String email,
        String mobileNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode
    ) {}
}
