package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.model.KycStatus;

import java.util.List;
import java.util.UUID;

public interface GetCustomerUseCase {
    Customer getById(UUID tenantId, UUID customerId);
    Customer getById(UUID customerId);
    Customer getByCifNumber(UUID tenantId, String cifNumber);
    Customer getByNationalId(UUID tenantId, String nationalId);
    Customer getByNationalId(String nationalId);
    Customer getByMobileNumber(String mobileNumber);
    Customer getByKeycloakUserId(UUID keycloakUserId);
    List<Customer> getAll();
    List<Customer> getAllByTenant(UUID tenantId);
    List<Customer> getByLifecycleStage(UUID tenantId, LifecycleStage lifecycleStage);
    List<Customer> getByKycStatus(UUID tenantId, KycStatus kycStatus);
}
