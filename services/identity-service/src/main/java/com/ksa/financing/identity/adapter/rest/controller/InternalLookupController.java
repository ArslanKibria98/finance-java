package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.model.UserStatus;
import com.ksa.financing.identity.domain.model.UserType;
import com.ksa.financing.identity.domain.port.in.LinkUserCustomerUseCase;
import com.ksa.financing.identity.domain.port.in.LinkUserCustomerUseCase.LinkResult;
import com.ksa.financing.identity.domain.port.in.LookupUserUseCase;
import com.ksa.financing.identity.domain.port.in.LookupUserUseCase.UserLookupResult;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Internal service-to-service user lookup endpoint.
 * Used by wallet-service / other services to resolve mobile, NID, or
 * Keycloak user id into a customerId + name for transfer flows etc.
 *
 * Mounted under /internal/** which is excluded from JWT and Casbin (per
 * SecurityConfig + ksa.authorization.skip-patterns). Trust comes from the
 * Docker/K8s internal network.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class InternalLookupController {

    private static final Set<String> SULLIS_FLOWS = Set.of("CANADA", "FOREIGN", "GUEST");
    private static final String DEFAULT_PUSH_PROVIDER = "fcm";
    private static final String SULLIS_PUSH_PROVIDER = "firebase-cloud-messaging-for-sullis";

    private final LookupUserUseCase lookupUserUseCase;
    private final LinkUserCustomerUseCase linkUserCustomerUseCase;
    private final UserIdentityRepository userIdentityRepository;
    private final KeycloakAdapterPort keycloakAdapter;

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    @GetMapping("/lookup")
    public ResponseEntity<LookupResponse> lookup(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String nid,
            @RequestParam(required = false) UUID keycloakId,
            @RequestParam(required = false) UUID customerId) {

        Optional<UserLookupResult> result = Optional.empty();

        if (mobile != null && !mobile.isBlank()) {
            String sanitizedMobile = mobile.trim();
            // Handle common issue where '+' is missing or decoded as ' '
            if (!sanitizedMobile.startsWith("+")) {
                sanitizedMobile = "+" + sanitizedMobile;
                log.debug("Auto-prefixed '+' to mobile: '{}'", sanitizedMobile);
            }
            result = lookupUserUseCase.lookupByMobile(sanitizedMobile);
        } else if (nid != null && !nid.isBlank()) {
            result = lookupUserUseCase.lookupByNationalId(nid.trim());
        } else if (keycloakId != null) {
            result = lookupUserUseCase.lookupByKeycloakUserId(keycloakId);
        } else if (customerId != null) {
            result = lookupUserUseCase.lookupByCustomerId(customerId);
        }

        if (result.isEmpty()) {
            log.debug("Internal lookup miss mobile={} nid={} keycloakId={} customerId={}", mobile, nid, keycloakId, customerId);
            return ResponseEntity.ok(new LookupResponse(false, null, null, null, null,
                    null, null, null, null, false));
        }

        UserLookupResult r = result.get();
        return ResponseEntity.ok(new LookupResponse(
                true,
                r.keycloakUserId(),
                r.customerId(),
                r.tenantId(),
                r.name(),
                r.firstName(),
                r.lastName(),
                maskMobile(r.mobileNumber()),
                r.status(),
                r.enabled()));
    }

    @PostMapping("/link-customer")
    public ResponseEntity<LinkCustomerResponse> linkCustomer(@RequestBody LinkCustomerRequest request) {
        if (request == null || request.keycloakUserId() == null || request.internalCustomerId() == null) {
            return ResponseEntity.badRequest().body(new LinkCustomerResponse(
                    false, null, null, "MISSING_IDS"));
        }
        LinkResult result = linkUserCustomerUseCase.link(request.keycloakUserId(), request.internalCustomerId());
        return ResponseEntity.ok(new LinkCustomerResponse(
                result.linked(),
                result.keycloakUserId(),
                result.internalCustomerId(),
                result.reason()));
    }

    public record LinkCustomerRequest(UUID keycloakUserId, UUID internalCustomerId) {}

    public record LinkCustomerResponse(boolean linked, UUID keycloakUserId,
                                       UUID internalCustomerId, String reason) {}

    /**
     * Upsert a user_identity_mapping row. Used by onboarding flows (Canada / Foreign /
     * Guest) whose Keycloak user is provisioned directly via the admin REST client
     * — they bypass {@code /auth/register} so the mapping is never written. Without
     * this call, mobile-based lookups (wallet recipient lookup, login by mobile)
     * return "not found".
     *
     * <p>Upsert key: {@code keycloakUserId}. Existing rows are updated in place;
     * new rows are inserted with the supplied fields.</p>
     */
    @PostMapping("/register-onboarded")
    @Transactional
    public ResponseEntity<RegisterOnboardedResponse> registerOnboarded(
            @RequestBody RegisterOnboardedRequest request) {
        if (request == null || request.keycloakUserId() == null
                || request.tenantId() == null) {
            return ResponseEntity.badRequest().body(new RegisterOnboardedResponse(
                    false, null, null, null, "MISSING_REQUIRED_IDS"));
        }

        var existing = userIdentityRepository.findByKeycloakUserId(request.keycloakUserId());
        UserIdentity identity = existing.orElseGet(UserIdentity::new);
        boolean isNew = existing.isEmpty();

        identity.setTenantId(request.tenantId());
        identity.setKeycloakUserId(request.keycloakUserId());
        if (request.keycloakRealm() != null) identity.setKeycloakRealm(request.keycloakRealm());
        if (request.keycloakUsername() != null) identity.setKeycloakUsername(request.keycloakUsername());
        if (request.mobileNumber() != null) identity.setMobileNumber(request.mobileNumber());
        if (request.internalCustomerId() != null) identity.setInternalCustomerId(request.internalCustomerId());
        if (isNew) {
            identity.setInternalUserId(UUID.randomUUID());
            identity.setUserType(UserType.CUSTOMER);
            identity.setStatus(UserStatus.PENDING_VERIFICATION);
            identity.setCreatedAt(Instant.now());
        }
        identity.setUpdatedAt(Instant.now());

        UserIdentity saved = userIdentityRepository.save(identity);
        log.info("Identity mapping {} keycloakUserId={} customerId={} mobile=****{}",
                isNew ? "CREATED" : "UPDATED",
                saved.getKeycloakUserId(),
                saved.getInternalCustomerId(),
                tail(saved.getMobileNumber()));
        return ResponseEntity.ok(new RegisterOnboardedResponse(
                true,
                saved.getKeycloakUserId(),
                saved.getInternalCustomerId(),
                saved.getMobileNumber(),
                isNew ? "CREATED" : "UPDATED"));
    }

    private String tail(String value) {
        if (value == null || value.length() < 4) return value;
        return value.substring(value.length() - 4);
    }

    public record RegisterOnboardedRequest(
            UUID tenantId,
            UUID keycloakUserId,
            String keycloakRealm,
            String keycloakUsername,
            String mobileNumber,
            UUID internalCustomerId) {}

    public record RegisterOnboardedResponse(boolean ok, UUID keycloakUserId,
                                            UUID internalCustomerId, String mobileNumber,
                                            String result) {}

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return mobile;
        return "****" + mobile.substring(mobile.length() - 4);
    }

    public record LookupResponse(
            boolean found,
            UUID keycloakUserId,
            UUID customerId,
            UUID tenantId,
            String name,
            String firstName,
            String lastName,
            String maskedMobile,
            String status,
            boolean enabled
    ) {}

    /**
     * Resolve which Novu push integration a customer's device tokens should be stored
     * under, based on the {@code onboarding_flow} Keycloak attribute.
     *
     * <p>Canada / Foreign / Guest users were onboarded via the Sullis app and must use
     * the Sullis FCM Firebase project. Everyone else (KSA NID flow) uses the default
     * {@code fcm} integration.
     *
     * <p>Called by notification-service on /api/v1/devices/register so the mobile app
     * does not need to know about Novu integration identifiers.
     */
    @GetMapping("/{customerId}/notification-provider")
    public ResponseEntity<NotificationProviderResponse> resolveNotificationProvider(
            @PathVariable UUID customerId) {
        Optional<UserLookupResult> lookup = lookupUserUseCase.lookupByCustomerId(customerId);
        if (lookup.isEmpty()) {
            log.warn("notification-provider lookup miss for customerId={} — defaulting to {}",
                    customerId, DEFAULT_PUSH_PROVIDER);
            return ResponseEntity.ok(new NotificationProviderResponse(
                    DEFAULT_PUSH_PROVIDER, null, false));
        }
        UUID kcUserId = lookup.get().keycloakUserId();
        String flow = null;
        try {
            flow = keycloakAdapter.getUserAttribute(realm, kcUserId, "onboarding_flow");
        } catch (Exception e) {
            log.warn("Keycloak onboarding_flow read failed for kcUserId={} — defaulting to {}: {}",
                    kcUserId, DEFAULT_PUSH_PROVIDER, e.getMessage());
        }
        String provider = flow != null && SULLIS_FLOWS.contains(flow.toUpperCase())
                ? SULLIS_PUSH_PROVIDER
                : DEFAULT_PUSH_PROVIDER;
        return ResponseEntity.ok(new NotificationProviderResponse(provider, flow, true));
    }

    public record NotificationProviderResponse(String providerId, String onboardingFlow, boolean resolved) {}
}
