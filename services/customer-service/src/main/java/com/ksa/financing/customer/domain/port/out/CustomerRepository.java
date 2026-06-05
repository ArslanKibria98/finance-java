package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.customer.domain.model.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(UUID tenantId, UUID id);
    Optional<Customer> findById(UUID id);
    Optional<Customer> findByIdWithLock(UUID id);
    Optional<Customer> findByCifNumber(UUID tenantId, String cifNumber);
    Optional<Customer> findByNationalId(UUID tenantId, String nationalId);
    Optional<Customer> findByNationalId(String nationalId);
    Optional<Customer> findByMobileNumber(String mobileNumber);
    Optional<Customer> findByKeycloakUserId(UUID keycloakUserId);
    Optional<Customer> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    boolean existsByNationalId(UUID tenantId, String nationalId);
    PageResponse<Customer> findAll(PageQuery query);
    PageResponse<Customer> findAllByTenantId(UUID tenantId, PageQuery query);
    PageResponse<Customer> findByLifecycleStage(UUID tenantId, String lifecycleStage, PageQuery query);
    PageResponse<Customer> findByKycStatus(UUID tenantId, String kycStatus, PageQuery query);
}
