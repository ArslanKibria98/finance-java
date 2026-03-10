package com.ksa.financing.globalprofile.application.usecase;

import com.ksa.financing.globalprofile.domain.model.KycStatus;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import com.ksa.financing.globalprofile.domain.port.in.LinkRegionalProfileUseCase;
import com.ksa.financing.globalprofile.domain.port.out.EventPublisherPort;
import com.ksa.financing.globalprofile.domain.port.out.GlobalCustomerRepository;
import com.ksa.financing.globalprofile.domain.port.out.RegionalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LinkRegionalProfileService implements LinkRegionalProfileUseCase {

    private final GlobalCustomerRepository globalCustomerRepository;
    private final RegionalProfileRepository regionalProfileRepository;
    private final EventPublisherPort eventPublisher;

    public LinkRegionalProfileService(GlobalCustomerRepository globalCustomerRepository,
                                       RegionalProfileRepository regionalProfileRepository,
                                       EventPublisherPort eventPublisher) {
        this.globalCustomerRepository = globalCustomerRepository;
        this.regionalProfileRepository = regionalProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RegionalProfile link(LinkRegionalProfileCommand command) {
        globalCustomerRepository.findByGlobalUid(command.globalUid())
            .orElseThrow(() -> new IllegalArgumentException("Global profile not found: " + command.globalUid()));

        var existing = regionalProfileRepository.findByGlobalUidAndCountry(command.globalUid(), command.countryCode());
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();

        RegionalProfile profile = new RegionalProfile();
        profile.setGlobalUid(command.globalUid());
        profile.setCountryCode(command.countryCode());
        profile.setRegionalCifNumber(command.regionalCifNumber());
        profile.setRegionalKycStatus(KycStatus.NOT_STARTED);
        profile.setPiiVaultRegion(command.countryCode());
        profile.setPiiVaultRecordId(command.piiVaultRecordId());
        profile.setKeycloakUserId(command.keycloakUserId());
        profile.setActive(true);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        RegionalProfile saved = regionalProfileRepository.save(profile);
        eventPublisher.publishRegionalProfileLinked(saved);
        return saved;
    }
}
