package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.domain.model.ApprovalConditionFieldDefinition;
import com.ksa.financing.product.domain.port.in.ManageApprovalConditionFieldsUseCase;
import com.ksa.financing.product.domain.port.in.ManageApprovalConditionFieldsUseCase.CreateFieldCommand;
import com.ksa.financing.product.domain.port.in.ManageApprovalConditionFieldsUseCase.UpdateFieldCommand;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approval-condition-fields")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Approval Condition Fields", description = "CRUD for approval workflow condition field definitions — populates dropdown in approval workflow UI")
public class ApprovalConditionFieldController {

    private final ManageApprovalConditionFieldsUseCase useCase;

    @SecuredEndpoint(obj = "approval-condition-fields", act = "read")
    @GetMapping
    @Operation(summary = "List all approval condition fields",
            description = "Returns active field definitions with predefined options (paginated)")
    public PageResponse<ApprovalConditionFieldDefinition> listFields(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        return useCase.listFieldDefinitions(tenantId, pageQuery);
    }

    @SecuredEndpoint(obj = "approval-condition-fields", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get a single approval condition field by ID")
    public ResponseEntity<ApprovalConditionFieldDefinition> getField(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var field = useCase.getFieldDefinition(tenantId, id);
        return ResponseEntity.ok(field);
    }

    @SecuredEndpoint(obj = "approval-condition-fields", act = "create")
    @PostMapping
    @Operation(summary = "Create a new approval condition field",
            description = "Adds a new field to the dropdown. field_key must be unique and snake_case (e.g. monthly_salary)")
    public ResponseEntity<ApprovalConditionFieldDefinition> createField(
            @Valid @RequestBody CreateFieldRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating approval condition field: {} for tenant: {}", request.fieldKey(), tenantId);

        var command = new CreateFieldCommand(
                request.fieldKey(), request.nameEn(), request.nameAr(),
                request.dataType(), request.sortOrder());

        var created = useCase.createFieldDefinition(tenantId, command);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();

        return ResponseEntity.created(location).body(created);
    }

    @SecuredEndpoint(obj = "approval-condition-fields", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update an approval condition field",
            description = "Update display name, data type, active status, or sort order. field_key cannot be changed.")
    public ResponseEntity<ApprovalConditionFieldDefinition> updateField(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFieldRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating approval condition field: {} for tenant: {}", id, tenantId);

        var command = new UpdateFieldCommand(
                request.nameEn(), request.nameAr(), request.dataType(),
                request.active(), request.sortOrder());

        var updated = useCase.updateFieldDefinition(tenantId, id, command);
        return ResponseEntity.ok(updated);
    }

    @SecuredEndpoint(obj = "approval-condition-fields", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an approval condition field",
            description = "Permanently removes a field from the dropdown. Existing workflow conditions using this field will NOT be affected.")
    public ResponseEntity<Void> deleteField(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deleting approval condition field: {} for tenant: {}", id, tenantId);

        useCase.deleteFieldDefinition(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTOs ──

    public record CreateFieldRequest(
        @NotBlank String fieldKey,
        @NotBlank String nameEn,
        @NotBlank String nameAr,
        @NotBlank String dataType,
        int sortOrder
    ) {}

    public record UpdateFieldRequest(
        @NotBlank String nameEn,
        @NotBlank String nameAr,
        @NotBlank String dataType,
        boolean active,
        int sortOrder
    ) {}

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
