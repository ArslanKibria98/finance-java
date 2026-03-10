package com.ksa.financing.piivault.adapter.rest.controller;

import com.ksa.financing.piivault.application.dto.PiiResponse;
import com.ksa.financing.piivault.application.dto.StorePiiRequest;
import com.ksa.financing.piivault.domain.model.PiiIndividual;
import com.ksa.financing.piivault.domain.port.in.DeletePiiUseCase;
import com.ksa.financing.piivault.domain.port.in.RetrievePiiUseCase;
import com.ksa.financing.piivault.domain.port.in.StorePiiUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for PII Vault operations.
 * <p>
 * This is an INTERNAL service — not exposed through Kong publicly.
 * All access requires JWT or service authentication. The PII Vault stores
 * encrypted PII data (AES-256-GCM) and maintains an immutable audit trail
 * with blockchain-style hash chaining.
 */
@RestController
@RequestMapping("/api/v1/pii")
@RequiredArgsConstructor
public class PiiVaultController {

    private final StorePiiUseCase storePiiUseCase;
    private final RetrievePiiUseCase retrievePiiUseCase;
    private final DeletePiiUseCase deletePiiUseCase;

    @SecuredEndpoint(obj = "pii", act = "create")
    @PostMapping("/individual")
    public ResponseEntity<PiiResponse> storePii(
            @Valid @RequestBody StorePiiRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var command = new StorePiiUseCase.StorePiiCommand(
                request.globalUid(),
                request.nationalId(),
                request.nationalIdType(),
                request.fullName(),
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.fullNameAr(),
                request.dateOfBirth(),
                request.gender(),
                request.nationalityCode(),
                request.mobile(),
                request.email(),
                request.countryCode()
        );

        PiiIndividual stored = storePiiUseCase.store(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(stored));
    }

    @SecuredEndpoint(obj = "pii", act = "read")
    @GetMapping("/individual/{globalUid}")
    public ResponseEntity<PiiResponse> retrievePii(
            @PathVariable UUID globalUid,
            @RequestParam(required = false) List<String> fields,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID accessorId = extractAccessorId(jwt);
        String accessorRole = extractAccessorRole(jwt);
        String accessorIp = extractAccessorIp(jwt);

        List<String> requestedFields = (fields != null && !fields.isEmpty())
                ? fields
                : List.of("ALL");

        PiiIndividual pii = retrievePiiUseCase.retrieve(
                globalUid, accessorId, accessorRole, accessorIp, "CUSTOMER_SERVICE", requestedFields);

        return ResponseEntity.ok(toResponse(pii));
    }

    @SecuredEndpoint(obj = "pii", act = "delete")
    @DeleteMapping("/individual/{globalUid}")
    public ResponseEntity<Void> deletePii(
            @PathVariable UUID globalUid,
            @RequestParam(required = false, defaultValue = "GDPR_RIGHT_TO_ERASURE") String reason,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID deletedBy = extractAccessorId(jwt);
        deletePiiUseCase.delete(globalUid, deletedBy, reason);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Private mapping helpers
    // -----------------------------------------------------------------------

    private PiiResponse toResponse(PiiIndividual pii) {
        return new PiiResponse(
                pii.getPiiId(),
                pii.getGlobalUid(),
                pii.getNationalId(),
                pii.getNationalIdType(),
                pii.getFullName(),
                pii.getFirstName(),
                pii.getLastName(),
                pii.getFullNameAr(),
                pii.getDateOfBirth(),
                pii.getGender(),
                pii.getNationalityCode(),
                pii.getMobile(),
                pii.getEmail(),
                pii.getCountryCode(),
                pii.getCreatedAt()
        );
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    /**
     * Extracts the accessor (user) ID from the JWT subject claim.
     * Throws BusinessException if subject claim is missing or invalid.
     */
    private UUID extractAccessorId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Invalid or missing subject claim in JWT token");
        }
    }

    /**
     * Extracts the accessor role from JWT realm_access.roles claim.
     */
    private String extractAccessorRole(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> realmMap) {
            Object roles = realmMap.get("roles");
            if (roles instanceof List<?> roleList && !roleList.isEmpty()) {
                return roleList.get(0).toString().toUpperCase();
            }
        }
        return "SERVICE";
    }

    /**
     * Extracts the accessor IP from the JWT or defaults to "internal-service".
     */
    private String extractAccessorIp(Jwt jwt) {
        Object ipClaim = jwt.getClaim("client_ip");
        return ipClaim != null ? ipClaim.toString() : "internal-service";
    }
}
