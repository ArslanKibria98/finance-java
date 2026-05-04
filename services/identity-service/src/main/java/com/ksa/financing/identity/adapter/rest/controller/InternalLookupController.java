package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.domain.port.in.LookupUserUseCase;
import com.ksa.financing.identity.domain.port.in.LookupUserUseCase.UserLookupResult;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
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

    private final LookupUserUseCase lookupUserUseCase;

    @GetMapping("/lookup")
    public ResponseEntity<LookupResponse> lookup(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String nid,
            @RequestParam(required = false) UUID keycloakId) {

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
        }

        if (result.isEmpty()) {
            log.debug("Internal lookup miss mobile={} nid={} keycloakId={}", mobile, nid, keycloakId);
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
}
