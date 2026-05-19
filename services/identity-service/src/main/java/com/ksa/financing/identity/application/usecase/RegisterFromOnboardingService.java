package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.model.UserStatus;
import com.ksa.financing.identity.domain.model.UserType;
import com.ksa.financing.identity.domain.port.in.RegisterFromOnboardingUseCase;
import com.ksa.financing.identity.domain.port.out.EventPublisherPort;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.identity.infrastructure.blacklist.LoginGuardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RegisterFromOnboardingService implements RegisterFromOnboardingUseCase {

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;
    private static final String CUSTOMER_ROLE = "customer";

    private static final Logger log = LoggerFactory.getLogger(RegisterFromOnboardingService.class);

    private final KeycloakAdapterPort keycloakAdapter;
    private final UserIdentityRepository userIdentityRepository;
    private final EventPublisherPort eventPublisher;
    private final LoginGuardService loginGuard;

    @Value("${platform.default-tenant-id:00000000-0000-0000-0000-000000000001}")
    private String defaultTenantId;

    public RegisterFromOnboardingService(KeycloakAdapterPort keycloakAdapter,
                                         UserIdentityRepository userIdentityRepository,
                                         EventPublisherPort eventPublisher,
                                         LoginGuardService loginGuard) {
        this.keycloakAdapter = keycloakAdapter;
        this.userIdentityRepository = userIdentityRepository;
        this.eventPublisher = eventPublisher;
        this.loginGuard = loginGuard;
    }

    @Override
    @Transactional
    public RegisterFromOnboardingResult register(RegisterFromOnboardingCommand command) {
        log.info("Starting onboarding registration for NID ending in: {}",
                maskNid(command.nationalId()));

        // Pre-token blacklist guard — block both NID and mobile before creating user / issuing token.
        loginGuard.verifyByNid(command.nationalId());
        loginGuard.verifyByMobile(command.mobileNumber());

        // Check if user already exists - if so, authenticate and return tokens
        var existing = userIdentityRepository.findByKeycloakUsername(command.nationalId());
        if (existing.isPresent()) {
            log.info("User already exists for NID: {}, generating tokens", maskNid(command.nationalId()));
            UserIdentity identity = existing.get();
            String tempPassword = UUID.randomUUID().toString();
            keycloakAdapter.resetPassword(realm, identity.getKeycloakUserId(), tempPassword);
            KeycloakAdapterPort.TokenResponse tokenResponse = keycloakAdapter.authenticate(
                    realm, command.nationalId(), tempPassword);
            return new RegisterFromOnboardingResult(
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
                    tokenResponse.expiresIn(),
                    identity.getKeycloakUserId().toString()
            );
        }

        // Step 1: Generate random secure password (user never sees this - auth is OTP-based)
        String password = UUID.randomUUID().toString();

        // Step 2: Create Keycloak user with NID as username
        String email = command.nationalId() + "@onboarding.local";
        KeycloakAdapterPort.KeycloakUser keycloakUser = keycloakAdapter.createUser(
                realm, command.nationalId(), email, password, command.firstName()
        );
        log.info("Keycloak user created with ID: {}", keycloakUser.keycloakUserId());

        // Step 3: Assign customer role
        keycloakAdapter.assignRole(realm, keycloakUser.keycloakUserId(), CUSTOMER_ROLE);
        log.info("Assigned '{}' role to Keycloak user: {}", CUSTOMER_ROLE, keycloakUser.keycloakUserId());

        // Step 3.1: Persist mobile_number + national_id as Keycloak user attributes so the
        // configured protocol mappers expose them as JWT claims for the BlacklistGuardFilter.
        Map<String, String> jwtAttributes = new HashMap<>();
        if (command.mobileNumber() != null && !command.mobileNumber().isBlank()) {
            jwtAttributes.put("mobile_number", command.mobileNumber());
        }
        jwtAttributes.put("national_id", command.nationalId());
        if (!jwtAttributes.isEmpty()) {
            try {
                keycloakAdapter.setUserAttributes(realm, keycloakUser.keycloakUserId(), jwtAttributes);
            } catch (Exception e) {
                // Keycloak 26+ Declarative User Profile can block unknown attributes if not explicitly configured.
                // We log this as a warning but continue registration so the user is not blocked from onboarding.
                log.warn("Failed to set Keycloak user attributes for userId: {} (continuing): {}",
                        keycloakUser.keycloakUserId(), e.getMessage());
            }
        }

        // Step 4: Create UserIdentity entity
        UserIdentity identity = new UserIdentity();
        identity.setTenantId(UUID.fromString(defaultTenantId));
        identity.setKeycloakUserId(keycloakUser.keycloakUserId());
        identity.setKeycloakRealm(realm);
        identity.setKeycloakUsername(command.nationalId());
        identity.setMobileNumber(command.mobileNumber());
        identity.setInternalUserId(UUID.randomUUID());
        if (command.globalUid() != null && !command.globalUid().isBlank()) {
            identity.setGlobalUid(UUID.fromString(command.globalUid()));
        }
        identity.setUserType(UserType.CUSTOMER);
        identity.setStatus(UserStatus.ACTIVE);

        // Step 5: Save entity and publish event
        UserIdentity saved = userIdentityRepository.save(identity);
        eventPublisher.publishUserRegistered(saved.getId(), saved.getTenantId(), command.fcmToken());
        log.info("UserIdentity saved with ID: {} and status: {}", saved.getId(), saved.getStatus());

        // Step 6: Authenticate to get JWT tokens
        KeycloakAdapterPort.TokenResponse tokenResponse = keycloakAdapter.authenticate(
                realm, command.nationalId(), password
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
