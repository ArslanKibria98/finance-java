package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import org.springframework.stereotype.Service;

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
    public Customer getByCifNumber(UUID tenantId, String cifNumber) {
        return customerRepository.findByCifNumber(tenantId, cifNumber)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with CIF: " + cifNumber));
    }

    @Override
    public Customer getByNationalId(UUID tenantId, String nationalId) {
        return customerRepository.findByNationalId(tenantId, nationalId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with national ID"));
    }
}
