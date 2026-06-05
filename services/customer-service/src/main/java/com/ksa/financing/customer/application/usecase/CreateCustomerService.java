package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.*;
import com.ksa.financing.customer.domain.port.in.CreateCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.customer.domain.port.out.EventPublisherPort;
import com.ksa.financing.customer.domain.port.out.GlobalProfilePort;
import com.ksa.financing.customer.domain.port.out.PiiVaultPort;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CreateCustomerService implements CreateCustomerUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateCustomerService.class);

    private final CustomerRepository customerRepository;
    private final GlobalProfilePort globalProfilePort;
    private final PiiVaultPort piiVaultPort;
    private final EventPublisherPort eventPublisher;

    public CreateCustomerService(CustomerRepository customerRepository,
                                 GlobalProfilePort globalProfilePort,
                                 PiiVaultPort piiVaultPort,
                                 EventPublisherPort eventPublisher) {
        this.customerRepository = customerRepository;
        this.globalProfilePort = globalProfilePort;
        this.piiVaultPort = piiVaultPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Customer create(CreateCustomerCommand command) {
        // Idempotency check: return existing customer if same key was already used
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            var existing = customerRepository.findByIdempotencyKey(
                    command.tenantId(), command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing customer for idempotencyKey={}", command.idempotencyKey());
                return existing.get();
            }
        }

        if (customerRepository.existsByNationalId(command.tenantId(), command.nationalId())) {
            throw new BusinessException(
                    ErrorCodes.Customer.ALREADY_EXISTS,
                    "Customer with this national ID already exists: " + command.nationalId(),
                    command.nationalId());
        }

        UUID globalUid;
        if (command.globalUid() != null) {
            globalUid = command.globalUid();
            log.info("Using pre-assigned globalUid from onboarding: {}", globalUid);
        } else {
            globalUid = globalProfilePort.createGlobalProfile(
                command.email(), command.mobileNumber(), "SAU"
            );
        }

        Map<String, String> piiFields = new HashMap<>();
        piiFields.put("nationalId", command.nationalId());
        piiFields.put("firstName", command.firstName());
        piiFields.put("lastName", command.lastName());
        piiFields.put("mobile", command.mobileNumber());
        if (command.email() != null) piiFields.put("email", command.email());
        try {
            piiVaultPort.storePii(globalUid, piiFields);
        } catch (Exception e) {
            // PII vault storage is non-critical for customer creation — log and continue
            log.warn("PII vault storage failed for globalUid: {} — continuing: {}", globalUid, e.getMessage());
        }

        Customer customer = new Customer();
        customer.setTenantId(command.tenantId());
        customer.setCifNumber(generateCifNumber());
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setNationalId(command.nationalId());
        customer.setNationalIdType(command.nationalIdType() != null ? command.nationalIdType() : "NID");
        customer.setFirstName(command.firstName());
        customer.setMiddleName(command.middleName());
        customer.setLastName(command.lastName());
        customer.setFirstNameAr(command.firstNameAr());
        customer.setLastNameAr(command.lastNameAr());
        String safeFirst = command.firstName() != null ? command.firstName() : "";
        String safeMiddle = command.middleName() != null ? command.middleName() + " " : "";
        String safeLast = command.lastName() != null ? command.lastName() : "";
        customer.setFullName((safeFirst + " " + safeMiddle + safeLast).trim());
        customer.setDateOfBirth(command.dateOfBirth() != null ? command.dateOfBirth() : java.time.LocalDate.of(1970, 1, 1));
        customer.setGender(parseEnumOrNull(Gender.class, command.gender(), "gender"));
        customer.setNationality(command.nationality());
        customer.setResidencyType(parseEnumOrDefault(ResidencyType.class, command.residencyType(),
                ResidencyType.VISITOR, "residencyType"));
        customer.setMobileNumber(command.mobileNumber());
        customer.setEmail(command.email());
        customer.setKycStatus(KycStatus.PENDING);
        customer.setPepStatus(PepStatus.PENDING);
        customer.setLifecycleStage(parseEnumOrDefault(LifecycleStage.class, command.lifecycleStage(),
                LifecycleStage.LEAD, "lifecycleStage"));
        customer.setGlobalUid(globalUid);
        customer.setKeycloakUserId(command.keycloakUserId());
        customer.setActive(true);
        customer.setCountry("SA");
        customer.setOnboardingFlow(command.onboardingFlow());
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            customer.setIdempotencyKey(command.idempotencyKey());
        }
        java.time.Instant now = java.time.Instant.now();
        customer.setLifecycleStageChangedAt(now);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);

        Customer saved = customerRepository.save(customer);
        try {
            eventPublisher.publishCustomerCreated(saved);
        } catch (Exception e) {
            log.warn("Failed to publish customer-created event for customerId: {} — continuing: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    private String generateCifNumber() {
        return "CIF" + System.currentTimeMillis() % 10000000000L;
    }

    private static <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Invalid value for " + fieldName + ": '" + value + "'. Allowed: " + java.util.Arrays.toString(type.getEnumConstants()),
                    value);
        }
    }

    private static <E extends Enum<E>> E parseEnumOrDefault(Class<E> type, String value, E fallback, String fieldName) {
        E parsed = parseEnumOrNull(type, value, fieldName);
        return parsed != null ? parsed : fallback;
    }
}
