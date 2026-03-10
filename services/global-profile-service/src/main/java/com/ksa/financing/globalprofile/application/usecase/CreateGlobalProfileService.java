package com.ksa.financing.globalprofile.application.usecase;

import com.ksa.financing.globalprofile.domain.model.CustomerType;
import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.GlobalKycAggregateStatus;
import com.ksa.financing.globalprofile.domain.port.in.CreateGlobalProfileUseCase;
import com.ksa.financing.globalprofile.domain.port.out.EventPublisherPort;
import com.ksa.financing.globalprofile.domain.port.out.GlobalCustomerRepository;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
public class CreateGlobalProfileService implements CreateGlobalProfileUseCase {

    private final GlobalCustomerRepository globalCustomerRepository;
    private final EventPublisherPort eventPublisher;

    public CreateGlobalProfileService(GlobalCustomerRepository globalCustomerRepository,
                                       EventPublisherPort eventPublisher) {
        this.globalCustomerRepository = globalCustomerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public GlobalCustomer create(CreateGlobalProfileCommand command) {
        String emailHash = (command.email() != null && !command.email().isBlank())
                ? sha256Hash(command.email().toLowerCase()) : null;
        String mobileHash = sha256Hash(command.mobile());

        // Dedup: check by email hash if available, otherwise by mobile hash
        if (emailHash != null) {
            var existing = globalCustomerRepository.findByEmailHash(emailHash);
            if (existing.isPresent()) {
                return existing.get();
            }
        } else {
            var existing = globalCustomerRepository.findByMobileHash(mobileHash);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Instant now = Instant.now();

        GlobalCustomer customer = new GlobalCustomer();
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setGlobalEmailHash(emailHash);
        customer.setGlobalMobileHash(mobileHash);
        customer.setPrimaryCountryCode(command.primaryCountryCode());
        customer.setGlobalKycStatus(GlobalKycAggregateStatus.NONE);
        customer.setActive(true);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);

        GlobalCustomer saved = globalCustomerRepository.save(customer);
        eventPublisher.publishGlobalCustomerCreated(saved);
        return saved;
    }

    private String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "SHA-256 algorithm not available", e);
        }
    }
}
