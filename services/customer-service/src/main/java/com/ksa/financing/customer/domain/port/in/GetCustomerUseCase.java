package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.Customer;
import java.util.UUID;

public interface GetCustomerUseCase {
    Customer getById(UUID tenantId, UUID customerId);
    Customer getByCifNumber(UUID tenantId, String cifNumber);
    Customer getByNationalId(UUID tenantId, String nationalId);
}
