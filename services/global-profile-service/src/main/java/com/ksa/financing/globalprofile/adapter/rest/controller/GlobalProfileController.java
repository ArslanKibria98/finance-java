package com.ksa.financing.globalprofile.adapter.rest.controller;

import com.ksa.financing.globalprofile.application.dto.CreateGlobalProfileRequest;
import com.ksa.financing.globalprofile.application.dto.GlobalProfileResponse;
import com.ksa.financing.globalprofile.application.dto.IssuePiiAccessTokenRequest;
import com.ksa.financing.globalprofile.application.dto.LinkRegionalProfileRequest;
import com.ksa.financing.globalprofile.application.dto.PiiAccessTokenResponse;
import com.ksa.financing.globalprofile.application.dto.RegionalProfileResponse;
import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.PiiAccessToken;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import com.ksa.financing.globalprofile.domain.port.in.CreateGlobalProfileUseCase;
import com.ksa.financing.globalprofile.domain.port.in.GetGlobalProfileUseCase;
import com.ksa.financing.globalprofile.domain.port.in.IssuePiiAccessTokenUseCase;
import com.ksa.financing.globalprofile.domain.port.in.LinkRegionalProfileUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for Global Profile operations.
 * <p>
 * This controller is called by other services internally (e.g., Customer Service,
 * KYC Orchestrator). It works with JWT authentication from Keycloak.
 * <p>
 * Global Profile Service stores ZERO PII — only hashes for deduplication.
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class GlobalProfileController {

    private final CreateGlobalProfileUseCase createGlobalProfileUseCase;
    private final GetGlobalProfileUseCase getGlobalProfileUseCase;
    private final LinkRegionalProfileUseCase linkRegionalProfileUseCase;
    private final IssuePiiAccessTokenUseCase issuePiiAccessTokenUseCase;

    @SecuredEndpoint(obj = "profiles", act = "create")
    @PostMapping
    public ResponseEntity<GlobalProfileResponse> createProfile(
            @Valid @RequestBody CreateGlobalProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var command = new CreateGlobalProfileUseCase.CreateGlobalProfileCommand(
                request.email(),
                request.mobile(),
                request.primaryCountryCode()
        );

        GlobalCustomer customer = createGlobalProfileUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(customer));
    }

    @SecuredEndpoint(obj = "profiles", act = "read")
    @GetMapping("/{globalUid}")
    public ResponseEntity<GlobalProfileResponse> getByGlobalUid(
            @PathVariable UUID globalUid,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        GlobalCustomer customer = getGlobalProfileUseCase.getByGlobalUid(globalUid);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "profiles", act = "read")
    @GetMapping("/by-email-hash/{emailHash}")
    public ResponseEntity<GlobalProfileResponse> getByEmailHash(
            @PathVariable String emailHash,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        GlobalCustomer customer = getGlobalProfileUseCase.findByEmail(emailHash);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "profiles", act = "read")
    @GetMapping("/by-mobile-hash/{mobileHash}")
    public ResponseEntity<GlobalProfileResponse> getByMobileHash(
            @PathVariable String mobileHash,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        GlobalCustomer customer = getGlobalProfileUseCase.findByMobile(mobileHash);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "profiles.regional", act = "create")
    @PostMapping("/{globalUid}/regional-profiles")
    public ResponseEntity<RegionalProfileResponse> linkRegionalProfile(
            @PathVariable UUID globalUid,
            @Valid @RequestBody LinkRegionalProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var command = new LinkRegionalProfileUseCase.LinkRegionalProfileCommand(
                globalUid,
                request.countryCode(),
                request.regionalCifNumber(),
                request.piiVaultRecordId(),
                request.keycloakUserId()
        );

        RegionalProfile profile = linkRegionalProfileUseCase.link(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRegionalResponse(profile));
    }

    @SecuredEndpoint(obj = "profiles.access-tokens", act = "create")
    @PostMapping("/{globalUid}/access-tokens")
    public ResponseEntity<PiiAccessTokenResponse> issueAccessToken(
            @PathVariable UUID globalUid,
            @Valid @RequestBody IssuePiiAccessTokenRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        String requesterIp = extractRequesterIp(jwt);

        var command = new IssuePiiAccessTokenUseCase.IssuePiiAccessTokenCommand(
                globalUid,
                request.allowedFields(),
                request.requesterId(),
                request.requesterRole(),
                requesterIp,
                request.accessPurpose(),
                request.relatedEntityType(),
                request.relatedEntityId()
        );

        PiiAccessToken token = issuePiiAccessTokenUseCase.issue(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTokenResponse(token));
    }

    // -----------------------------------------------------------------------
    // Private mapping helpers
    // -----------------------------------------------------------------------

    private GlobalProfileResponse toResponse(GlobalCustomer customer) {
        return new GlobalProfileResponse(
                customer.getGlobalUid(),
                customer.getCustomerType() != null ? customer.getCustomerType().name() : null,
                customer.getPrimaryCountryCode(),
                customer.getGlobalKycStatus() != null ? customer.getGlobalKycStatus().name() : null,
                customer.getGlobalRiskGrade(),
                customer.isPepFlag(),
                customer.isSanctionsFlag(),
                customer.isFraudFlag(),
                customer.isActive(),
                customer.getCustomerSegment(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private RegionalProfileResponse toRegionalResponse(RegionalProfile profile) {
        return new RegionalProfileResponse(
                profile.getRegionalProfileId(),
                profile.getGlobalUid(),
                profile.getCountryCode(),
                profile.getRegionalCifNumber(),
                profile.getRegionalKycStatus() != null ? profile.getRegionalKycStatus().name() : null,
                profile.getKycVerifiedAt(),
                profile.getKycExpiryDate(),
                profile.getPiiVaultRegion(),
                profile.getPiiVaultRecordId(),
                profile.isActive(),
                profile.getCreatedAt()
        );
    }

    private PiiAccessTokenResponse toTokenResponse(PiiAccessToken token) {
        return new PiiAccessTokenResponse(
                token.getTokenId(),
                token.getAccessToken(),
                token.getGlobalUid(),
                token.getAllowedFields(),
                token.getAccessPurpose(),
                token.getIssuedAt(),
                token.getExpiresAt()
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
     * Extracts requester IP from the JWT claim or defaults to "internal-service".
     * For service-to-service calls, the IP may not be present.
     */
    private String extractRequesterIp(Jwt jwt) {
        if (jwt == null) {
            return "internal-service";
        }
        Object ipClaim = jwt.getClaim("client_ip");
        return ipClaim != null ? ipClaim.toString() : "unknown";
    }
}
