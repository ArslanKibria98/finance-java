package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.ledger.application.dto.*;
import com.ksa.financing.ledger.application.mapper.CoaConfigurationMapper;
import com.ksa.financing.ledger.domain.port.in.ManageCoaConfigurationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coa-configurations")
@RequiredArgsConstructor
@Tag(name = "COA Configuration", description = "Product-wise COA account mappings")
public class CoaConfigurationController {

    private final ManageCoaConfigurationUseCase useCase;
    private final CoaConfigurationMapper mapper;

    @SecuredEndpoint(obj = "ledger.coa-config", act = "create")
    @PostMapping
    @Operation(summary = "Create COA configuration profile")
    public ResponseEntity<CoaConfigurationProfileResponse> createProfile(
            @Valid @RequestBody CreateCoaConfigurationProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var profile = useCase.createProfile(new ManageCoaConfigurationUseCase.CreateProfileCommand(
                tenantId,
                request.productCode(),
                request.profileName(),
                request.effectiveFrom(),
                request.effectiveTo()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(profile));
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "read")
    @GetMapping("/{profileId}")
    @Operation(summary = "Get COA configuration profile by id")
    public ResponseEntity<CoaConfigurationProfileResponse> getProfile(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(mapper.toResponse(useCase.getProfile(extractTenantId(jwt), profileId)));
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "read")
    @GetMapping
    @Operation(summary = "List COA profiles by product")
    public ResponseEntity<List<CoaConfigurationProfileResponse>> listByProduct(
            @RequestParam String productCode,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var response = useCase.listProfilesByProduct(tenantId, productCode).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "read")
    @GetMapping("/{profileId}/mappings")
    @Operation(summary = "Get profile mappings")
    public ResponseEntity<List<CoaConfigurationMappingResponse>> getMappings(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var response = useCase.getMappings(tenantId, profileId).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "update")
    @PutMapping("/{profileId}/fields")
    @Operation(summary = "Select COA fields for product profile")
    public ResponseEntity<List<CoaConfigurationMappingResponse>> selectFields(
            @PathVariable UUID profileId,
            @Valid @RequestBody SelectCoaFieldsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var mappings = useCase.selectFields(new ManageCoaConfigurationUseCase.SelectFieldsCommand(
                tenantId,
                profileId,
                request.fieldKeys()
        ));
        return ResponseEntity.ok(mappings.stream().map(mapper::toResponse).toList());
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "update")
    @PutMapping("/{profileId}/assign-accounts")
    @Operation(summary = "Assign accounts to selected COA fields")
    public ResponseEntity<List<CoaConfigurationMappingResponse>> assignAccounts(
            @PathVariable UUID profileId,
            @Valid @RequestBody AssignCoaAccountsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var assignments = request.assignments().stream()
                .map(a -> new ManageCoaConfigurationUseCase.AssignmentItem(a.fieldKey(), a.accountCode()))
                .toList();
        var mappings = useCase.assignAccounts(new ManageCoaConfigurationUseCase.AssignAccountsCommand(
                tenantId,
                profileId,
                assignments
        ));
        return ResponseEntity.ok(mappings.stream().map(mapper::toResponse).toList());
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "update")
    @PutMapping("/{profileId}")
    @Operation(summary = "Upsert product COA mappings")
    public ResponseEntity<List<CoaConfigurationMappingResponse>> upsertMappings(
            @PathVariable UUID profileId,
            @Valid @RequestBody UpsertCoaMappingsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var commandItems = request.mappings().stream()
                .map(item -> new ManageCoaConfigurationUseCase.MappingItem(
                        item.fieldKey(),
                        item.accountCode(),
                        item.mandatoryOverride(),
                        item.notes()
                ))
                .toList();
        var mappings = useCase.upsertMappings(new ManageCoaConfigurationUseCase.UpsertMappingsCommand(
                tenantId,
                profileId,
                commandItems
        ));
        return ResponseEntity.ok(mappings.stream().map(mapper::toResponse).toList());
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "manage")
    @PostMapping("/{profileId}/validate")
    @Operation(summary = "Validate profile mappings")
    public ResponseEntity<CoaConfigurationValidationResponse> validate(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var validation = useCase.validate(extractTenantId(jwt), profileId);
        return ResponseEntity.ok(mapper.toResponse(validation));
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "manage")
    @PostMapping("/{profileId}/activate")
    @Operation(summary = "Activate COA configuration profile")
    public ResponseEntity<CoaConfigurationProfileResponse> activate(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(mapper.toResponse(useCase.activate(extractTenantId(jwt), profileId)));
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "manage")
    @PostMapping("/{profileId}/deactivate")
    @Operation(summary = "Deactivate COA configuration profile")
    public ResponseEntity<CoaConfigurationProfileResponse> deactivate(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(mapper.toResponse(useCase.deactivate(extractTenantId(jwt), profileId)));
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "No tenant_id claim found in JWT token");
        }
        try {
            return UUID.fromString(tenantClaim);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Invalid tenant_id claim format in JWT token");
        }
    }
}
