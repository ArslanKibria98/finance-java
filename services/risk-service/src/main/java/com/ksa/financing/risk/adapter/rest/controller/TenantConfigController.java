package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.tenant.TenantConfig;
import com.ksa.financing.risk.domain.port.in.ManageTenantConfigUseCase;
import com.ksa.financing.risk.domain.port.in.ManageTenantConfigUseCase.CreateTenantConfigCommand;
import com.ksa.financing.risk.domain.port.in.ManageTenantConfigUseCase.UpdateTenantConfigCommand;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/tenant-configs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Configuration", description = "APIs for managing tenant risk configurations")
public class TenantConfigController {

    private final ManageTenantConfigUseCase manageTenantConfigUseCase;

    @SecuredEndpoint(obj = "risk.tenant-config", act = "create")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tenant risk configuration")
    public TenantConfig create(
            @Valid @RequestBody CreateTenantConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Creating tenant config for tenant: {}", tenantId);
        return manageTenantConfigUseCase.create(tenantId, new CreateTenantConfigCommand(
                request.tenantName(),
                request.tenantNameAr(),
                request.customerRiskEnabled(),
                request.businessRiskEnabled(),
                request.loanRiskEnabled()
        ));
    }

    @SecuredEndpoint(obj = "risk.tenant-config", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update a tenant risk configuration")
    public TenantConfig update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Updating tenant config: {} for tenant: {}", id, tenantId);
        return manageTenantConfigUseCase.update(tenantId, id, new UpdateTenantConfigCommand(
                request.tenantName(),
                request.tenantNameAr(),
                request.customerRiskEnabled(),
                request.businessRiskEnabled(),
                request.loanRiskEnabled()
        ));
    }

    @SecuredEndpoint(obj = "risk.tenant-config", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get tenant config by ID")
    public TenantConfig getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageTenantConfigUseCase.getById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.tenant-config", act = "read")
    @GetMapping("/current")
    @Operation(summary = "Get tenant config for the current tenant from JWT")
    public TenantConfig getByTenantId(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageTenantConfigUseCase.getByTenantId(tenantId);
    }

    @SecuredEndpoint(obj = "risk.tenant-config", act = "read")
    @GetMapping
    @Operation(summary = "Get all tenant configurations")
    public List<TenantConfig> getAll(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Listing all tenant configs for tenant: {}", tenantId);
        return manageTenantConfigUseCase.getAll();
    }

    @SecuredEndpoint(obj = "risk.tenant-config", act = "manage")
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a tenant configuration")
    public void activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Activating tenant config: {} for tenant: {}", id, tenantId);
        manageTenantConfigUseCase.activate(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.tenant-config", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a tenant configuration")
    public void deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Deactivating tenant config: {} for tenant: {}", id, tenantId);
        manageTenantConfigUseCase.deactivate(tenantId, id);
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

    record CreateTenantConfigRequest(
            String tenantName,
            String tenantNameAr,
            boolean customerRiskEnabled,
            boolean businessRiskEnabled,
            boolean loanRiskEnabled
    ) {}

    record UpdateTenantConfigRequest(
            String tenantName,
            String tenantNameAr,
            Boolean customerRiskEnabled,
            Boolean businessRiskEnabled,
            Boolean loanRiskEnabled
    ) {}
}
