package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.LoginWithEmailPinUseCase;
import com.ksa.financing.identity.domain.port.out.CustomerLookupPort;
import com.ksa.financing.identity.domain.port.out.EventPublisherPort;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Email + bcrypt PIN login service. Implements the lookup-by-email,
 * bcrypt-verify, password-reset, password-grant chain used by the
 * Canada / Foreign / Guest onboarding flows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginWithEmailPinService implements LoginWithEmailPinUseCase {

    private static final String PIN_HASH_ATTRIBUTE = "pin_hash";
    private static final String MOBILE_ATTRIBUTE = "mobile_number";
    private static final String FLOW_ATTRIBUTE = "onboarding_flow";
    private static final String COMPLETE_ATTRIBUTE = "onboarding_complete";

    private static final BCryptPasswordEncoder PIN_ENCODER = new BCryptPasswordEncoder();

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    private final KeycloakAdapterPort keycloakAdapter;
    private final UserIdentityRepository userIdentityRepository;
    private final EventPublisherPort eventPublisher;
    private final CustomerLookupPort customerLookupPort;

    @Override
    public LoginWithEmailPinResult login(LoginWithEmailPinCommand command) {
        log.info("Email+PIN login attempt for email: {}", maskEmail(command.email()));

        // 1. Lookup Keycloak user by email
        var kcUser = keycloakAdapter.findUserByEmail(realm, command.email())
                .orElseThrow(() -> {
                    log.warn("No Keycloak user found for email: {}", maskEmail(command.email()));
                    return new NotFoundException("User", command.email());
                });

        UUID keycloakUserId = kcUser.keycloakUserId();
        String keycloakUsername = kcUser.username();

        // 2. Read the bcrypt-hashed PIN attribute
        String storedHash = keycloakAdapter.getUserAttribute(realm, keycloakUserId, PIN_HASH_ATTRIBUTE);
        if (storedHash == null || storedHash.isBlank()) {
            log.warn("pin_hash attribute missing for kcUserId={}", keycloakUserId);
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_NOT_SET,
                    "PIN has not been set for this account");
        }

        // 3. Verify the PIN with bcrypt
        if (!PIN_ENCODER.matches(command.pin(), storedHash)) {
            log.warn("Invalid PIN for kcUserId={} email={}", keycloakUserId, maskEmail(command.email()));
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_INVALID,
                    "Invalid PIN provided");
        }

        // 4. Issue a Keycloak-real JWT by resetting the password to a random secret
        //    and immediately doing a password-grant authenticate.
        String tempPassword = UUID.randomUUID().toString();
        keycloakAdapter.resetPassword(realm, keycloakUserId, tempPassword);
        KeycloakAdapterPort.TokenResponse token = keycloakAdapter.authenticate(
                realm, keycloakUsername, tempPassword);

        // 5. Read onboarding metadata for the response
        String mobile  = keycloakAdapter.getUserAttribute(realm, keycloakUserId, MOBILE_ATTRIBUTE);
        String flow    = keycloakAdapter.getUserAttribute(realm, keycloakUserId, FLOW_ATTRIBUTE);
        String complete = keycloakAdapter.getUserAttribute(realm, keycloakUserId, COMPLETE_ATTRIBUTE);
        boolean isComplete = "true".equalsIgnoreCase(complete);

        log.info("Email+PIN login success: kcUserId={} flow={} complete={}",
                keycloakUserId, flow, isComplete);

        // Resolve customerId / pepStatus / name from user_identity_mapping +
        // customer-service. Mapping carries internal_customer_id once the user
        // finishes the selfie step; until then customerId stays null.
        String customerId = null;
        String pepStatus = null;
        String name = null;
        var identityOpt = userIdentityRepository.findByKeycloakUserId(keycloakUserId);
        if (identityOpt.isPresent()) {
            var identity = identityOpt.get();
            if (identity.getInternalCustomerId() != null) {
                customerId = identity.getInternalCustomerId().toString();
                var lookup = customerLookupPort.resolveCustomerById(customerId, token.accessToken());
                if (lookup.isPresent()) {
                    pepStatus = lookup.get().pepStatus();
                    name = lookup.get().name();
                }
            }
            // Publish USER_LOGIN event for notification orchestration (same as mobile PIN path).
            UUID customerUuid = identity.getInternalCustomerId() != null
                    ? identity.getInternalCustomerId()
                    : identity.getInternalUserId();
            eventPublisher.publishUserLogin(
                    identity.getInternalUserId(),
                    identity.getTenantId(),
                    customerUuid,
                    mobile != null ? mobile : identity.getMobileNumber(),
                    name != null ? name : token.name()
            );
        } else {
            log.warn("UserIdentity row missing for keycloakUserId={} — skipping USER_LOGIN publish + customer resolve", keycloakUserId);
        }
        if (name == null) {
            name = token.name();
        }

        return new LoginWithEmailPinResult(
                token.accessToken(),
                token.refreshToken(),
                token.expiresIn(),
                "Bearer",
                keycloakUserId.toString(),
                customerId,
                pepStatus,
                name,
                command.email(),
                mobile,
                flow,
                isComplete
        );
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
