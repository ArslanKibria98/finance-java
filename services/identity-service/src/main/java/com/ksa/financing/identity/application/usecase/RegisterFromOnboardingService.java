package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.model.UserStatus;
import com.ksa.financing.identity.domain.model.UserType;
import com.ksa.financing.identity.domain.port.in.RegisterFromOnboardingUseCase;
import com.ksa.financing.identity.domain.port.out.EventPublisherPort;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterFromOnboardingService implements RegisterFromOnboardingUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterFromOnboardingService.class);
    private static final String REALM = "CompanyRealm";
    private static final String CUSTOMER_ROLE = "customer";

    private final KeycloakAdapterPort keycloakAdapter;
    private final UserIdentityRepository userIdentityRepository;
    private final EventPublisherPort eventPublisher;

    public RegisterFromOnboardingService(KeycloakAdapterPort keycloakAdapter,
                                         UserIdentityRepository userIdentityRepository,
                                         EventPublisherPort eventPublisher) {
        this.keycloakAdapter = keycloakAdapter;
        this.userIdentityRepository = userIdentityRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RegisterFromOnboardingResult register(RegisterFromOnboardingCommand command) {
        log.info("Starting onboarding registration for NID ending in: {}",
                maskNid(command.nationalId()));

        // Step 1: Generate random secure password (user never sees this - auth is OTP-based)
        String password = UUID.randomUUID().toString();

        // Step 2: Create Keycloak user with NID as username
        String email = command.nationalId() + "@onboarding.local";
        KeycloakAdapterPort.KeycloakUser keycloakUser = keycloakAdapter.createUser(
                REALM, command.nationalId(), email, password
        );
        log.info("Keycloak user created with ID: {}", keycloakUser.keycloakUserId());

        // Step 3: Assign customer role
        keycloakAdapter.assignRole(REALM, keycloakUser.keycloakUserId(), CUSTOMER_ROLE);
        log.info("Assigned '{}' role to Keycloak user: {}", CUSTOMER_ROLE, keycloakUser.keycloakUserId());

        // Step 4: Create UserIdentity entity
        UserIdentity identity = new UserIdentity();
        identity.setTenantId(UUID.randomUUID());
        identity.setKeycloakUserId(keycloakUser.keycloakUserId());
        identity.setKeycloakRealm(REALM);
        identity.setKeycloakUsername(command.nationalId());
        identity.setInternalUserId(UUID.randomUUID());
        if (command.globalUid() != null && !command.globalUid().isBlank()) {
            identity.setGlobalUid(UUID.fromString(command.globalUid()));
        }
        identity.setUserType(UserType.CUSTOMER);
        identity.setStatus(UserStatus.ACTIVE);

        // Step 5: Save entity and publish event
        UserIdentity saved = userIdentityRepository.save(identity);
        eventPublisher.publishUserRegistered(saved.getId(), saved.getTenantId());
        log.info("UserIdentity saved with ID: {} and status: {}", saved.getId(), saved.getStatus());

        // Step 6: Authenticate to get JWT tokens
        KeycloakAdapterPort.TokenResponse tokenResponse = keycloakAdapter.authenticate(
                REALM, command.nationalId(), password
        );
        log.info("JWT tokens obtained for onboarding user: {}", keycloakUser.keycloakUserId());

        // Step 7: Return result with tokens and keycloak user ID
        return new RegisterFromOnboardingResult(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                keycloakUser.keycloakUserId().toString()
        );
    }

    private String maskNid(String nid) {
        if (nid == null || nid.length() < 4) {
            return "***";
        }
        return "***" + nid.substring(nid.length() - 4);
    }
}
