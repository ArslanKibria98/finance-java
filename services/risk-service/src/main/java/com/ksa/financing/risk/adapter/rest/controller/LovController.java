package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.lov.LovCategoryType;
import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.model.lov.LovSet;
import com.ksa.financing.risk.domain.port.in.ManageLovUseCase;
import com.ksa.financing.risk.domain.port.in.ManageLovUseCase.CreateLovEntryCommand;
import com.ksa.financing.risk.domain.port.in.ManageLovUseCase.CreateLovSetCommand;
import com.ksa.financing.risk.domain.port.in.ManageLovUseCase.UpdateLovEntryCommand;
import com.ksa.financing.risk.domain.port.in.ManageLovUseCase.UpdateLovSetCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/lov")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LOV Management", description = "APIs for managing LOV sets and entries")
public class LovController {

    private final ManageLovUseCase manageLovUseCase;

    // ===== LOV SET OPERATIONS =====

    @SecuredEndpoint(obj = "risk.lov", act = "create")
    @PostMapping("/sets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a LOV set")
    public LovSet createLovSet(
            @Valid @RequestBody CreateLovSetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Creating LOV set: {} for tenant: {}", request.code(), tenantId);
        return manageLovUseCase.createLovSet(tenantId, new CreateLovSetCommand(
                request.code(),
                request.nameEn(),
                request.nameAr(),
                request.categoryType()
        ));
    }

    @SecuredEndpoint(obj = "risk.lov", act = "update")
    @PutMapping("/sets/{id}")
    @Operation(summary = "Update a LOV set")
    public LovSet updateLovSet(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLovSetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Updating LOV set: {} for tenant: {}", id, tenantId);
        return manageLovUseCase.updateLovSet(tenantId, id, new UpdateLovSetCommand(
                request.nameEn(),
                request.nameAr(),
                request.categoryType()
        ));
    }

    @SecuredEndpoint(obj = "risk.lov", act = "read")
    @GetMapping("/sets/{id}")
    @Operation(summary = "Get LOV set by ID")
    public LovSet getLovSetById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageLovUseCase.getLovSetById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.lov", act = "read")
    @GetMapping("/sets")
    @Operation(summary = "Get all LOV sets")
    public List<LovSet> getAllLovSets(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageLovUseCase.getAllLovSets(tenantId);
    }

    @SecuredEndpoint(obj = "risk.lov", act = "read")
    @GetMapping("/sets/active")
    @Operation(summary = "Get all active LOV sets")
    public List<LovSet> getActiveLovSets(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageLovUseCase.getActiveLovSets(tenantId);
    }

    @SecuredEndpoint(obj = "risk.lov", act = "manage")
    @PostMapping("/sets/{id}/deactivate")
    @Operation(summary = "Deactivate a LOV set")
    public void deactivateLovSet(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Deactivating LOV set: {} for tenant: {}", id, tenantId);
        manageLovUseCase.deactivateLovSet(tenantId, id);
    }

    // ===== LOV ENTRY OPERATIONS =====

    @SecuredEndpoint(obj = "risk.lov", act = "create")
    @PostMapping("/sets/{lovSetId}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a LOV entry within a set")
    public LovEntry createLovEntry(
            @PathVariable UUID lovSetId,
            @Valid @RequestBody CreateLovEntryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Creating LOV entry in set: {} for tenant: {}", lovSetId, tenantId);
        return manageLovUseCase.createLovEntry(tenantId, lovSetId, new CreateLovEntryCommand(
                request.factorCode(),
                request.labelEn(),
                request.labelAr(),
                request.factorWeight(),
                request.riskStatus(),
                request.sortOrder()
        ));
    }

    @SecuredEndpoint(obj = "risk.lov", act = "update")
    @PutMapping("/entries/{id}")
    @Operation(summary = "Update a LOV entry")
    public LovEntry updateLovEntry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLovEntryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Updating LOV entry: {} for tenant: {}", id, tenantId);
        return manageLovUseCase.updateLovEntry(tenantId, id, new UpdateLovEntryCommand(
                request.labelEn(),
                request.labelAr(),
                request.factorWeight(),
                request.riskStatus(),
                request.sortOrder()
        ));
    }

    @SecuredEndpoint(obj = "risk.lov", act = "read")
    @GetMapping("/sets/{lovSetId}/entries")
    @Operation(summary = "Get all entries for a LOV set")
    public List<LovEntry> getEntriesByLovSet(
            @PathVariable UUID lovSetId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageLovUseCase.getEntriesByLovSet(tenantId, lovSetId);
    }

    @SecuredEndpoint(obj = "risk.lov", act = "read")
    @GetMapping("/sets/{lovSetId}/entries/active")
    @Operation(summary = "Get active entries for a LOV set")
    public List<LovEntry> getActiveEntriesByLovSet(
            @PathVariable UUID lovSetId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageLovUseCase.getActiveEntriesByLovSet(tenantId, lovSetId);
    }

    @SecuredEndpoint(obj = "risk.lov", act = "manage")
    @PostMapping("/entries/{id}/deactivate")
    @Operation(summary = "Deactivate a LOV entry")
    public void deactivateLovEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Deactivating LOV entry: {} for tenant: {}", id, tenantId);
        manageLovUseCase.deactivateLovEntry(tenantId, id);
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

    // === Request Records ===

    record CreateLovSetRequest(
            String code,
            String nameEn,
            String nameAr,
            LovCategoryType categoryType
    ) {}

    record UpdateLovSetRequest(
            String nameEn,
            String nameAr,
            LovCategoryType categoryType
    ) {}

    record CreateLovEntryRequest(
            String factorCode,
            String labelEn,
            String labelAr,
            BigDecimal factorWeight,
            String riskStatus,
            int sortOrder
    ) {}

    record UpdateLovEntryRequest(
            String labelEn,
            String labelAr,
            BigDecimal factorWeight,
            String riskStatus,
            Integer sortOrder
    ) {}
}
