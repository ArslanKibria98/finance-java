package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.model.PepStatus;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.CustomerPepAnswerRepository;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.customer.domain.port.out.CustomerBlockRepository;
import com.ksa.financing.customer.domain.model.CustomerBlock;
import com.ksa.financing.infra.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetCustomerService implements GetCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerPepAnswerRepository customerPepAnswerRepository;
    private final CustomerBlockRepository customerBlockRepository;

    public GetCustomerService(
            CustomerRepository customerRepository,
            CustomerPepAnswerRepository customerPepAnswerRepository,
            CustomerBlockRepository customerBlockRepository) {
        this.customerRepository = customerRepository;
        this.customerPepAnswerRepository = customerPepAnswerRepository;
        this.customerBlockRepository = customerBlockRepository;
    }

    @Override
    public Customer getById(UUID tenantId, UUID customerId) {
        var customer = customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return normalizePepStatus(customer);
    }

    @Override
    public Customer getById(UUID customerId) {
        var customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return normalizePepStatus(customer);
    }

    @Override
    public Customer getByCifNumber(UUID tenantId, String cifNumber) {
        var customer = customerRepository.findByCifNumber(tenantId, cifNumber)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with CIF: " + cifNumber));
        return normalizePepStatus(customer);
    }

    @Override
    public Customer getByNationalId(UUID tenantId, String nationalId) {
        var customer = customerRepository.findByNationalId(tenantId, nationalId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with national ID"));
        return normalizePepStatus(customer);
    }

    @Override
    public Customer getByNationalId(String nationalId) {
        var customer = customerRepository.findByNationalId(nationalId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with national ID"));
        return normalizePepStatus(customer);
    }

    @Override
    public Customer getByMobileNumber(String mobileNumber) {
        var customer = customerRepository.findByMobileNumber(mobileNumber)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with mobile number"));
        return normalizePepStatus(customer);
    }

    @Override
    public Customer getByKeycloakUserId(UUID keycloakUserId) {
        var customer = customerRepository.findByKeycloakUserId(keycloakUserId)
            .orElseThrow(() -> NotFoundException.forEntity("Customer", keycloakUserId.toString()));
        return normalizePepStatus(customer);
    }

    private Customer normalizePepStatus(Customer customer) {
        var hasPepAnswers = customerPepAnswerRepository
                .findByCustomer(customer.getTenantId(), customer.getId())
                .isPresent();

        // COMPLETED only when the customer has actually submitted PEP/EDD answers
        // (onboarding EDD form for isPep=true, or post-login PEP screen).
        // Otherwise PENDING — the mobile app must still prompt the post-login PEP screen.
        PepStatus targetStatus = hasPepAnswers ? PepStatus.COMPLETED : PepStatus.PENDING;

        if (customer.getPepStatus() != targetStatus) {
            customer.setPepStatus(targetStatus);
            customer = customerRepository.save(customer);
        }

        // Populate block codes
        List<String> blockCodes = customerBlockRepository.findAllActiveByCustomerId(customer.getId())
                .stream()
                .map(CustomerBlock::getBlockCode)
                .toList();
        customer.setBlockCodes(blockCodes);

        return customer;
    }

    @Override
    public PageResponse<Customer> getAll(PageQuery query) {
        return customerRepository.findAll(query).map(this::normalizePepStatus);
    }

    @Override
    public PageResponse<Customer> getAllByTenant(UUID tenantId, PageQuery query) {
        return customerRepository.findAllByTenantId(tenantId, query).map(this::normalizePepStatus);
    }

    @Override
    public PageResponse<Customer> getByLifecycleStage(UUID tenantId, LifecycleStage lifecycleStage, PageQuery query) {
        return customerRepository.findByLifecycleStage(tenantId, lifecycleStage.name(), query).map(this::normalizePepStatus);
    }

    @Override
    public PageResponse<Customer> getByKycStatus(UUID tenantId, KycStatus kycStatus, PageQuery query) {
        return customerRepository.findByKycStatus(tenantId, kycStatus.name(), query).map(this::normalizePepStatus);
    }
}
