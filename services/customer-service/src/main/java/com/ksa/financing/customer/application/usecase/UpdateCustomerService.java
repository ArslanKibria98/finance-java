package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.model.RiskGrade;
import com.ksa.financing.customer.domain.port.in.UpdateCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.customer.domain.port.out.EventPublisherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class UpdateCustomerService implements UpdateCustomerUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateCustomerService.class);

    private final CustomerRepository customerRepository;
    private final EventPublisherPort eventPublisher;

    public UpdateCustomerService(CustomerRepository customerRepository,
                                 EventPublisherPort eventPublisher) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Customer updateKycStatus(UUID tenantId, UUID customerId, KycStatus status) {
        Customer customer = customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        String oldStatus = customer.getKycStatus().name();
        customer.setKycStatus(status);
        Customer saved = customerRepository.save(customer);
        try {
            eventPublisher.publishKycStatusChanged(saved, oldStatus, status.name());
        } catch (Exception e) {
            log.warn("Failed to publish kyc-status-changed event for customerId: {} — continuing: {}",
                    saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Override
    @Transactional
    public Customer updateLifecycleStage(UUID tenantId, UUID customerId, LifecycleStage stage) {
        Customer customer = customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        String oldStage = customer.getLifecycleStage() != null ? customer.getLifecycleStage().name() : null;
        customer.setLifecycleStage(stage);
        Customer saved = customerRepository.save(customer);
        try {
            eventPublisher.publishLifecycleStageChanged(saved, oldStage, stage.name());
        } catch (Exception e) {
            log.warn("Failed to publish lifecycle-stage-changed event for customerId: {} — continuing: {}",
                    saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Override
    @Transactional
    public Customer updateRiskGrade(UUID tenantId, UUID customerId, RiskGrade grade) {
        Customer customer = (tenantId != null
                ? customerRepository.findById(tenantId, customerId)
                : customerRepository.findById(customerId))
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        customer.setRiskGrade(grade);
        customer.setRiskGradeUpdatedAt(java.time.Instant.now());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer updatePepFlag(UUID tenantId, UUID customerId, boolean pepFlag) {
        Customer customer = customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        customer.setPepFlag(pepFlag);
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer update(UUID tenantId, UUID customerId, UpdateCustomerCommand command) {
        Customer customer = customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        if (command.firstName() != null) customer.setFirstName(command.firstName());
        if (command.lastName() != null) customer.setLastName(command.lastName());
        if (command.firstNameAr() != null) customer.setFirstNameAr(command.firstNameAr());
        if (command.lastNameAr() != null) customer.setLastNameAr(command.lastNameAr());
        if (command.email() != null) customer.setEmail(command.email());
        if (command.mobileNumber() != null) customer.setMobileNumber(command.mobileNumber());
        if (command.addressLine1() != null) customer.setAddressLine1(command.addressLine1());
        if (command.addressLine2() != null) customer.setAddressLine2(command.addressLine2());
        if (command.city() != null) customer.setCity(command.city());
        if (command.region() != null) customer.setRegion(command.region());
        if (command.postalCode() != null) customer.setPostalCode(command.postalCode());
        if (command.profilePicture() != null) customer.setProfilePicture(command.profilePicture());

        Customer saved = customerRepository.save(customer);
        try {
            eventPublisher.publishCustomerUpdated(saved);
        } catch (Exception e) {
            log.warn("Failed to publish customer-updated event for customerId: {} — continuing: {}",
                    saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Override
    @Transactional
    public Customer linkKeycloakUser(String mobileNumber, UUID keycloakUserId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with mobileNumber: " + mobileNumber));
        if (customer.getKeycloakUserId() != null) {
            log.info("Customer {} already linked to Keycloak user {}", customer.getId(), customer.getKeycloakUserId());
            return customer;
        }
        customer.setKeycloakUserId(keycloakUserId);
        Customer saved = customerRepository.save(customer);
        log.info("Linked keycloakUserId={} to customerId={}", keycloakUserId, saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Customer linkKeycloakUserByCustomerId(UUID customerId, UUID keycloakUserId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + customerId));
        if (customer.getKeycloakUserId() != null) {
            log.info("Customer {} already linked to Keycloak user {}", customerId, customer.getKeycloakUserId());
            return customer;
        }
        customer.setKeycloakUserId(keycloakUserId);
        Customer saved = customerRepository.save(customer);
        log.info("Linked keycloakUserId={} to customerId={}", keycloakUserId, saved.getId());
        return saved;
    }
}
