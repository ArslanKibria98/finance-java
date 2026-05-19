package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.model.UserStatus;
import com.ksa.financing.identity.domain.port.in.LoginWithPinUseCase;
import com.ksa.financing.identity.domain.port.out.CustomerLookupPort;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.identity.infrastructure.blacklist.LoginGuardService;
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
    private final LoginGuardService loginGuard;

    @Override
    public LoginWithPinResult login(LoginWithPinCommand command) {
        log.info("PIN login attempt for NID ending in: {}", maskNid(command.nationalId()));

        // Step 0: Pre-token blacklist guard — block early before hitting Keycloak.
        loginGuard.verifyByNid(command.nationalId());

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
        var customerLookup = customerLookupPort
                .resolveCustomerByNationalId(command.nationalId(), tokenResponse.accessToken())
                .orElseGet(() -> {
                    log.warn("Could not resolve customer-service ID for NID: {}, falling back to internalUserId",
                            maskNid(command.nationalId()));
                    return new CustomerLookupPort.CustomerLookupResult(
                            identity.getInternalUserId() != null ? identity.getInternalUserId().toString() : null,
                            null,
                            null
                    );
                });

        return new LoginWithPinResult(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                customerLookup.customerId(),
                customerLookup.pepStatus(),
                identity.getKeycloakUsername(),
                identity.getMobileNumber(),
                resolveName(tokenResponse.name(), customerLookup.name())
        );
    }

    @Override
    public LoginWithPinResult loginWithMobile(LoginWithMobileCommand command) {
        log.info("Mobile PIN login attempt for mobile ending in: {}", maskMobile(command.mobileNumber()));

        // Step 0: Pre-token blacklist guard — mobile + (later) NID once we resolve the identity.
        loginGuard.verifyByMobile(command.mobileNumber());

        // Step 1: Find user by mobile number
        UserIdentity identity = userIdentityRepository.findByMobileNumber(command.mobileNumber())
                .orElseThrow(() -> {
                    log.warn("No user found for mobile ending in: {}", maskMobile(command.mobileNumber()));
                    return new NotFoundException("User", command.mobileNumber());
                });

        // Re-check guard against the resolved NID (mobile alone may not be blacklisted but NID could be)
        loginGuard.verifyByNid(identity.getKeycloakUsername());

        // Step 2: Verify account is active
        if (identity.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login attempt for inactive account. Mobile: {}, Status: {}",
                    maskMobile(command.mobileNumber()), identity.getStatus());
            throw new BusinessException(
                    ErrorCodes.Identity.ACCOUNT_INACTIVE,
                    "Account is not active. Current status: " + identity.getStatus());
        }

        // Step 3: Get stored PIN from Keycloak
        String storedPin = keycloakAdapter.getUserAttribute(
                realm, identity.getKeycloakUserId(), PIN_ATTRIBUTE);

        if (storedPin == null || storedPin.isBlank()) {
            log.warn("PIN not set for user. Mobile: {}", maskMobile(command.mobileNumber()));
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_NOT_SET,
                    "PIN has not been set for this account");
        }

        // Step 4: Verify PIN
        if (!storedPin.equals(command.pin())) {
            log.warn("Invalid PIN attempt for mobile: {}", maskMobile(command.mobileNumber()));
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_INVALID,
                    "Invalid PIN provided");
        }

        // Step 5: Generate temp password, reset in Keycloak, and authenticate
        String tempPassword = UUID.randomUUID().toString();
        keycloakAdapter.resetPassword(realm, identity.getKeycloakUserId(), tempPassword);

        // keycloakUsername = nationalId (used as Keycloak username)
        KeycloakAdapterPort.TokenResponse tokenResponse = keycloakAdapter.authenticate(
                realm, identity.getKeycloakUsername(), tempPassword);

        log.info("Mobile PIN login successful for mobile ending in: {}", maskMobile(command.mobileNumber()));

        // Resolve customer ID
        String customerId = null;
        String pepStatus = null;
        String customerName = null;
        if (identity.getKeycloakUsername() != null) {
            var lookup = customerLookupPort
                    .resolveCustomerByNationalId(identity.getKeycloakUsername(), tokenResponse.accessToken())
                    .orElseGet(() -> new CustomerLookupPort.CustomerLookupResult(
                            identity.getInternalUserId() != null ? identity.getInternalUserId().toString() : null,
                            null,
                            null
                    ));
            customerId = lookup.customerId();
            pepStatus = lookup.pepStatus();
            customerName = lookup.name();
        }

        return new LoginWithPinResult(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                customerId,
                pepStatus,
                identity.getKeycloakUsername(),
                identity.getMobileNumber(),
                resolveName(tokenResponse.name(), customerName)
        );
    }

    private String resolveName(String jwtName, String customerName) {
        if (customerName != null && !customerName.isBlank()) {
            return customerName;
        }
        return jwtName;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }

    private String maskNid(String nid) {
        if (nid == null || nid.length() < 4) {
            return "***";
        }
        return "***" + nid.substring(nid.length() - 4);
    }
}
