package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.port.in.LinkUserCustomerUseCase;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkUserCustomerService implements LinkUserCustomerUseCase {

    private final UserIdentityRepository userIdentityRepository;

    @Override
    @Transactional
    public LinkResult link(UUID keycloakUserId, UUID internalCustomerId) {
        if (keycloakUserId == null || internalCustomerId == null) {
            return new LinkResult(false, keycloakUserId, internalCustomerId, "MISSING_IDS");
        }

        var existing = userIdentityRepository.findByKeycloakUserId(keycloakUserId);
        if (existing.isEmpty()) {
            log.warn("Identity link skipped — UserIdentity not found for keycloakUserId={}", keycloakUserId);
            return new LinkResult(false, keycloakUserId, internalCustomerId, "USER_IDENTITY_NOT_FOUND");
        }

        UserIdentity identity = existing.get();
        if (internalCustomerId.equals(identity.getInternalCustomerId())) {
            return new LinkResult(true, keycloakUserId, internalCustomerId, "ALREADY_LINKED");
        }

        identity.setInternalCustomerId(internalCustomerId);
        userIdentityRepository.save(identity);
        log.info("Linked UserIdentity keycloakUserId={} → internalCustomerId={}", keycloakUserId, internalCustomerId);
        return new LinkResult(true, keycloakUserId, internalCustomerId, "LINKED");
    }
}
