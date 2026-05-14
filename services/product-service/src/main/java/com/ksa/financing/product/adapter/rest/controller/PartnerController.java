package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreatePartnerRequest;
import com.ksa.financing.product.adapter.rest.request.UpdatePartnerRequest;
import com.ksa.financing.product.adapter.rest.response.PartnerResponse;
import com.ksa.financing.product.domain.model.Partner;
import com.ksa.financing.product.domain.port.in.ManagePartnerUseCase;
import com.ksa.financing.product.domain.port.in.ManagePartnerUseCase.CreatePartnerCommand;
import com.ksa.financing.product.domain.port.in.ManagePartnerUseCase.UpdatePartnerCommand;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Partners", description = "Partner management — Admin CRUD for partner profiles")
public class PartnerController {

    private final ManagePartnerUseCase managePartnerUseCase;

    @SecuredEndpoint(obj = "partners", act = "create")
    @PostMapping
    @Operation(summary = "Create partner", description = "Creates a new partner in ACTIVE status")
    public ResponseEntity<PartnerResponse> createPartner(
            @Valid @RequestBody CreatePartnerRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        log.info("Creating partner with code: {} for tenantId: {}", request.partnerCode(), tenantId);

        var command = new CreatePartnerCommand(
                tenantId,
                request.partnerCode(),
                request.nameEn(),
                request.nameAr(),
                request.email(),
                request.phone(),
                request.contactPerson(),
                request.logoUrl(),
                userId
        );

        var partner = managePartnerUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(partner));
    }

    @SecuredEndpoint(obj = "partners", act = "read")
    @GetMapping
    @Operation(summary = "List partners. Optional ?search= filters by partnerCode, nameEn, nameAr, email, phone, contactPerson, status (case-insensitive LIKE).",
            description = "Returns all partners for the tenant")
    public ResponseEntity<List<PartnerResponse>> listPartners(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing partners for tenantId: {} (search={})", tenantId, search);

        String term = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        var partners = managePartnerUseCase.listByTenant(tenantId);
        var response = partners.stream()
                .map(this::toResponse)
                .filter(p -> term == null
                        || c(p.partnerCode(), term)
                        || c(p.nameEn(), term)
                        || c(p.nameAr(), term)
                        || c(p.email(), term)
                        || c(p.phone(), term)
                        || c(p.contactPerson(), term)
                        || c(p.status(), term))
                .toList();
        return ResponseEntity.ok(response);
    }

    private static boolean c(String f, String t) {
        return f != null && f.toLowerCase().contains(t);
    }

    @SecuredEndpoint(obj = "partners", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get partner", description = "Returns a single partner by ID")
    public ResponseEntity<PartnerResponse> getPartner(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Getting partner id: {} for tenantId: {}", id, tenantId);

        var partner = managePartnerUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(partner));
    }

    @SecuredEndpoint(obj = "partners", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update partner", description = "Updates partner profile information")
    public ResponseEntity<PartnerResponse> updatePartner(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePartnerRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        log.info("Updating partner id: {} for tenantId: {}", id, tenantId);

        var command = new UpdatePartnerCommand(
                request.nameEn(),
                request.nameAr(),
                request.email(),
                request.phone(),
                request.contactPerson(),
                request.logoUrl(),
                userId
        );

        var partner = managePartnerUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(partner));
    }

    @SecuredEndpoint(obj = "partners", act = "manage")
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate partner", description = "Activates an INACTIVE or SUSPENDED partner")
    public ResponseEntity<Void> activatePartner(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Activating partner id: {} for tenantId: {}", id, tenantId);
        managePartnerUseCase.activate(tenantId, id);
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "partners", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate partner", description = "Deactivates a partner")
    public ResponseEntity<Void> deactivatePartner(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deactivating partner id: {} for tenantId: {}", id, tenantId);
        managePartnerUseCase.deactivate(tenantId, id);
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "partners", act = "manage")
    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend partner", description = "Suspends an ACTIVE partner")
    public ResponseEntity<Void> suspendPartner(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Suspending partner id: {} for tenantId: {}", id, tenantId);
        managePartnerUseCase.suspend(tenantId, id);
        return ResponseEntity.ok().build();
    }

    // === Helpers ===

    private PartnerResponse toResponse(Partner p) {
        return new PartnerResponse(
                p.getId(), p.getTenantId(), p.getPartnerCode(),
                p.getNameEn(), p.getNameAr(),
                p.getEmail(), p.getPhone(), p.getContactPerson(), p.getLogoUrl(),
                p.getStatus(),
                p.getCreatedAt(), p.getUpdatedAt(),
                p.getCreatedBy(), p.getUpdatedBy(),
                p.getVersion()
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

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
