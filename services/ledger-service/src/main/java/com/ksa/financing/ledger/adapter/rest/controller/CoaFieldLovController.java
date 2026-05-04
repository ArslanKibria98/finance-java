package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.application.dto.CoaFieldLovResponse;
import com.ksa.financing.ledger.application.dto.CreateCoaFieldLovRequest;
import com.ksa.financing.ledger.application.dto.UpdateCoaFieldLovRequest;
import com.ksa.financing.ledger.application.mapper.CoaConfigurationMapper;
import com.ksa.financing.ledger.domain.port.in.ManageCoaFieldLovUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lovs/coa-fields")
@RequiredArgsConstructor
@Tag(name = "COA Field LOV", description = "COA field names managed as LOVs")
public class CoaFieldLovController {

    private final ManageCoaFieldLovUseCase useCase;
    private final CoaConfigurationMapper mapper;

    @SecuredEndpoint(obj = "ledger.coa-fields", act = "create")
    @PostMapping
    @Operation(summary = "Create COA field LOV")
    public ResponseEntity<CoaFieldLovResponse> create(
            @Valid @RequestBody CreateCoaFieldLovRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var created = useCase.create(new ManageCoaFieldLovUseCase.CreateCoaFieldLovCommand(
                tenantId,
                request.fieldKey(),
                request.fieldLabelEn(),
                request.fieldLabelAr(),
                request.category(),
                request.mandatoryDefault(),
                request.displayOrder()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @SecuredEndpoint(obj = "ledger.coa-fields", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get COA field LOV by id")
    public ResponseEntity<CoaFieldLovResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(mapper.toResponse(useCase.getById(tenantId, id)));
    }

    @SecuredEndpoint(obj = "ledger.coa-fields", act = "read")
    @GetMapping
    @Operation(summary = "List COA field LOVs",
            description = "Returns all COA field LOVs. Pass activeOnly=true to filter only active entries.")
    public ResponseEntity<PageResponse<CoaFieldLovResponse>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery
    ) {
        var tenantId = extractTenantId(jwt);
        var response = useCase.list(tenantId, activeOnly, pageQuery).map(mapper::toResponse);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "ledger.coa-fields", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update COA field LOV")
    public ResponseEntity<CoaFieldLovResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCoaFieldLovRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var tenantId = extractTenantId(jwt);
        var updated = useCase.update(tenantId, id, new ManageCoaFieldLovUseCase.UpdateCoaFieldLovCommand(
                request.fieldLabelEn(),
                request.fieldLabelAr(),
                request.category(),
                request.mandatoryDefault(),
                request.displayOrder()
        ));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @SecuredEndpoint(obj = "ledger.coa-fields", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate COA field LOV",
            description = "Sets the field status to INACTIVE. Returns the updated field so the caller can verify the status change.")
    public ResponseEntity<CoaFieldLovResponse> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var updated = useCase.deactivate(extractTenantId(jwt), id);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @SecuredEndpoint(obj = "ledger.coa-fields", act = "manage")
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate COA field LOV",
            description = "Sets the field status to ACTIVE. Returns the updated field so the caller can verify the status change.")
    public ResponseEntity<CoaFieldLovResponse> activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var updated = useCase.activate(extractTenantId(jwt), id);
        return ResponseEntity.ok(mapper.toResponse(updated));
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
