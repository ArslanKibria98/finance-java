package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.model.KycStatus;

import java.util.UUID;

public interface GetCustomerUseCase {
    Customer getById(UUID tenantId, UUID customerId);
    Customer getById(UUID customerId);
    Customer getByCifNumber(UUID tenantId, String cifNumber);
    Customer getByNationalId(UUID tenantId, String nationalId);
    Customer getByNationalId(String nationalId);
    Customer getByMobileNumber(String mobileNumber);
    Customer getByKeycloakUserId(UUID keycloakUserId);
    PageResponse<Customer> getAll(PageQuery query);
    PageResponse<Customer> getAllByTenant(UUID tenantId, PageQuery query);
    PageResponse<Customer> getByLifecycleStage(UUID tenantId, LifecycleStage lifecycleStage, PageQuery query);
    PageResponse<Customer> getByKycStatus(UUID tenantId, KycStatus kycStatus, PageQuery query);
}
