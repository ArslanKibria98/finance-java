package com.ksa.financing.identity.application.service;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.port.in.LookupUserUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LookupUserService implements LookupUserUseCase {

    private final UserIdentityRepository userIdentityRepository;
    private final KeycloakAdapterPort keycloakAdapter;

    @Value("${keycloak.realm:CompanyRealm}")
    private String defaultRealm;

    @Override
    public Optional<UserLookupResult> lookupByMobile(String mobileNumber) {
        return userIdentityRepository.findByMobileNumber(mobileNumber).map(this::enrich);
    }

    @Override
    public Optional<UserLookupResult> lookupByKeycloakUserId(UUID keycloakUserId) {
        return userIdentityRepository.findByKeycloakUserId(keycloakUserId).map(this::enrich);
    }

    @Override
    public Optional<UserLookupResult> lookupByNationalId(String nationalId) {
        return userIdentityRepository.findByKeycloakUsername(nationalId).map(this::enrich);
    }

    @Override
    public Optional<UserLookupResult> lookupByCustomerId(UUID internalCustomerId) {
        return userIdentityRepository.findByInternalCustomerId(internalCustomerId).map(this::enrich);
    }

    private UserLookupResult enrich(UserIdentity identity) {
        String firstName = null;
        String lastName = null;
        String fullName = null;
        boolean enabled = false;

        try {
            String realm = identity.getKeycloakRealm() != null ? identity.getKeycloakRealm() : defaultRealm;
            KeycloakAdapterPort.KeycloakUserDetails details =
                    keycloakAdapter.getUserDetails(realm, identity.getKeycloakUserId());
            if (details != null) {
                firstName = details.firstName();
                lastName = details.lastName();
                fullName = combineName(firstName, lastName);
                enabled = details.enabled();
            }
        } catch (Exception ex) {
            log.warn("Failed to enrich user details from Keycloak for keycloakUserId={}: {}",
                    identity.getKeycloakUserId(), ex.getMessage());
        }

        return new UserLookupResult(
                identity.getKeycloakUserId(),
                identity.getInternalCustomerId(),
                identity.getTenantId(),
                fullName,
                firstName,
                lastName,
                identity.getMobileNumber(),
                identity.getKeycloakUsername(),
                identity.getStatus() != null ? identity.getStatus().name() : null,
                enabled);
    }

    private String combineName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) sb.append(first.trim());
        if (last != null && !last.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(last.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
