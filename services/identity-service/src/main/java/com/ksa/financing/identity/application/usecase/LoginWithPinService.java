package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.model.UserStatus;
import com.ksa.financing.identity.domain.port.in.LoginWithPinUseCase;
import com.ksa.financing.identity.domain.port.out.CustomerLookupPort;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginWithPinService implements LoginWithPinUseCase {

    private static final String PIN_ATTRIBUTE = "app_pin";

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    private final KeycloakAdapterPort keycloakAdapter;
    private final UserIdentityRepository userIdentityRepository;
    private final CustomerLookupPort customerLookupPort;

    @Override
    public LoginWithPinResult login(LoginWithPinCommand command) {
        log.info("PIN login attempt for NID ending in: {}", maskNid(command.nationalId()));

        // Step 1: Find user by NID (keycloakUsername = nationalId)
        UserIdentity identity = userIdentityRepository.findByKeycloakUsername(command.nationalId())
                .orElseThrow(() -> {
                    log.warn("No user found for NID ending in: {}", maskNid(command.nationalId()));
                    return new NotFoundException("User", command.nationalId());
                });

        // Step 2: Verify account is active
        if (identity.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login attempt for inactive account. NID: {}, Status: {}",
                    maskNid(command.nationalId()), identity.getStatus());
            throw new BusinessException(
                    ErrorCodes.Identity.ACCOUNT_INACTIVE,
                    "Account is not active. Current status: " + identity.getStatus());
        }

        // Step 3: Get stored PIN from Keycloak
        String storedPin = keycloakAdapter.getUserAttribute(
                realm, identity.getKeycloakUserId(), PIN_ATTRIBUTE);

        if (storedPin == null || storedPin.isBlank()) {
            log.warn("PIN not set for user. NID: {}", maskNid(command.nationalId()));
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_NOT_SET,
                    "PIN has not been set for this account");
        }

        // Step 4: Verify PIN
        if (!storedPin.equals(command.pin())) {
            log.warn("Invalid PIN attempt for NID: {}", maskNid(command.nationalId()));
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_INVALID,
                    "Invalid PIN provided");
        }

        // Step 5: Generate temp password, reset in Keycloak, and authenticate
        String tempPassword = UUID.randomUUID().toString();
        keycloakAdapter.resetPassword(realm, identity.getKeycloakUserId(), tempPassword);

        KeycloakAdapterPort.TokenResponse tokenResponse = keycloakAdapter.authenticate(
                realm, command.nationalId(), tempPassword);

        log.info("PIN login successful for NID ending in: {}", maskNid(command.nationalId()));

        // Resolve actual customer-service ID by NID
        String customerId = customerLookupPort
                .resolveCustomerIdByNationalId(command.nationalId(), tokenResponse.accessToken())
                .orElseGet(() -> {
                    log.warn("Could not resolve customer-service ID for NID: {}, falling back to internalUserId",
                            maskNid(command.nationalId()));
                    return identity.getInternalUserId() != null
                            ? identity.getInternalUserId().toString()
                            : null;
                });

        return new LoginWithPinResult(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                customerId,
                identity.getKeycloakUsername(),
                identity.getMobileNumber()
        );
    }

    private String maskNid(String nid) {
        if (nid == null || nid.length() < 4) {
            return "***";
        }
        return "***" + nid.substring(nid.length() - 4);
    }
}
