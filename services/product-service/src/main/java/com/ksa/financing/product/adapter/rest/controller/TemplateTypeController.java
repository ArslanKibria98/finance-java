package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreateTemplateTypeRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateTemplateTypeRequest;
import com.ksa.financing.product.adapter.rest.response.TemplateTypeResponse;
import com.ksa.financing.product.application.mapper.TemplateTypeMapper;
import com.ksa.financing.product.domain.model.TemplateType;
import com.ksa.financing.product.domain.port.in.ManageTemplateTypeUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/template-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Template Types", description = "Template type management (contract_type, notification_type, etc.)")
public class TemplateTypeController {

    private final ManageTemplateTypeUseCase manageTemplateTypeUseCase;

    @SecuredEndpoint(obj = "template-types", act = "read")
    @GetMapping
    @Operation(summary = "List all template types", description = "Returns all active template types for the tenant (paginated)")
    public PageResponse<TemplateTypeResponse> listAll(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        return manageTemplateTypeUseCase.listAll(tenantId, pageQuery)
                .map(TemplateTypeMapper::toResponse);
    }

    @SecuredEndpoint(obj = "template-types", act = "read")
    @GetMapping("/category/{category}")
    @Operation(summary = "List template types by category", description = "Returns template types filtered by category (e.g., contract_type, notification_type)")
    public ResponseEntity<List<TemplateTypeResponse>> listByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var types = manageTemplateTypeUseCase.listByCategory(tenantId, category);
        var response = types.stream().map(TemplateTypeMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "template-types", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get template type by ID")
    public ResponseEntity<TemplateTypeResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var type = manageTemplateTypeUseCase.getById(tenantId, id);
        return ResponseEntity.ok(TemplateTypeMapper.toResponse(type));
    }

    @SecuredEndpoint(obj = "template-types", act = "create")
    @PostMapping
    @Operation(summary = "Create template type", description = "Creates a new template type entry")
    public ResponseEntity<TemplateTypeResponse> create(
            @Valid @RequestBody CreateTemplateTypeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating template type nameEn={} category={} for tenant={}", request.nameEn(), request.category(), tenantId);

        var type = new TemplateType();
        type.setNameEn(request.nameEn());
        type.setNameAr(request.nameAr());
        type.setCategory(request.category());

        var created = manageTemplateTypeUseCase.create(tenantId, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateTypeMapper.toResponse(created));
    }

    @SecuredEndpoint(obj = "template-types", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update template type")
    public ResponseEntity<TemplateTypeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateTypeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating template type id={} for tenant={}", id, tenantId);

        var updates = new TemplateType();
        updates.setNameEn(request.nameEn());
        updates.setNameAr(request.nameAr());
        updates.setCategory(request.category());
        updates.setActive(request.active());

        var updated = manageTemplateTypeUseCase.update(tenantId, id, updates);
        return ResponseEntity.ok(TemplateTypeMapper.toResponse(updated));
    }

    @SecuredEndpoint(obj = "template-types", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template type")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deleting template type id={} for tenant={}", id, tenantId);

        manageTemplateTypeUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
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
}
