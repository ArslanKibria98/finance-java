package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetCustomerService implements GetCustomerUseCase {

    private final CustomerRepository customerRepository;

    public GetCustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer getById(UUID tenantId, UUID customerId) {
        return customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    @Override
    public Customer getById(UUID customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    @Override
    public Customer getByCifNumber(UUID tenantId, String cifNumber) {
        return customerRepository.findByCifNumber(tenantId, cifNumber)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with CIF: " + cifNumber));
    }

    @Override
    public Customer getByNationalId(UUID tenantId, String nationalId) {
        return customerRepository.findByNationalId(tenantId, nationalId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with national ID"));
    }

    @Override
    public Customer getByNationalId(String nationalId) {
        return customerRepository.findByNationalId(nationalId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with national ID"));
    }

    @Override
    public Customer getByMobileNumber(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with mobile number"));
    }

    @Override
    public Customer getByKeycloakUserId(UUID keycloakUserId) {
        return customerRepository.findByKeycloakUserId(keycloakUserId)
            .orElseThrow(() -> NotFoundException.forEntity("Customer", keycloakUserId.toString()));
    }

    @Override
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @Override
    public List<Customer> getAllByTenant(UUID tenantId) {
        return customerRepository.findAllByTenantId(tenantId);
    }

    @Override
    public List<Customer> getByLifecycleStage(UUID tenantId, LifecycleStage lifecycleStage) {
        return customerRepository.findByLifecycleStage(tenantId, lifecycleStage.name());
    }

    @Override
    public List<Customer> getByKycStatus(UUID tenantId, KycStatus kycStatus) {
        return customerRepository.findByKycStatus(tenantId, kycStatus.name());
    }
}
