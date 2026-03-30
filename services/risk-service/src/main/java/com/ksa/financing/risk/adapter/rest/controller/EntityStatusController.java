package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;
import com.ksa.financing.risk.domain.model.status.EntityStatusRecord;
import com.ksa.financing.risk.domain.port.in.ManageEntityStatusUseCase;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/entity-status")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entity Status", description = "APIs for managing entity risk, account, and compliance statuses")
public class EntityStatusController {

    private final ManageEntityStatusUseCase manageEntityStatusUseCase;

    @SecuredEndpoint(obj = "risk.entity-status", act = "read")
    @GetMapping("/{entityReference}/current")
    @Operation(summary = "Get current status for an entity")
    public EntityStatusRecord getCurrentStatus(
            @PathVariable String entityReference,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageEntityStatusUseCase.getCurrentStatus(tenantId, entityReference);
    }

    @SecuredEndpoint(obj = "risk.entity-status", act = "read")
    @GetMapping("/{entityReference}/history")
    @Operation(summary = "Get status history for an entity")
    public List<EntityStatusRecord> getStatusHistory(
            @PathVariable String entityReference,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageEntityStatusUseCase.getStatusHistory(tenantId, entityReference);
    }

    @SecuredEndpoint(obj = "risk.entity-status", act = "manage")
    @PostMapping("/{entityReference}/account-status")
    @Operation(summary = "Update account status for an entity")
    public EntityStatusRecord updateAccountStatus(
            @PathVariable String entityReference,
            @Valid @RequestBody UpdateAccountStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID changedBy = UUID.fromString(jwt.getSubject());
        var accountStatus = AccountStatus.valueOf(request.accountStatus());
        log.info("Updating account status for entity: {} to: {} tenant: {}", entityReference, accountStatus, tenantId);
        return manageEntityStatusUseCase.updateAccountStatus(
                tenantId, entityReference, accountStatus, request.reason(), changedBy);
    }

    @SecuredEndpoint(obj = "risk.entity-status", act = "manage")
    @PostMapping("/{entityReference}/compliance-status")
    @Operation(summary = "Update compliance status for an entity")
    public EntityStatusRecord updateComplianceStatus(
            @PathVariable String entityReference,
            @Valid @RequestBody UpdateComplianceStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID changedBy = UUID.fromString(jwt.getSubject());
        var complianceStatus = ComplianceStatus.valueOf(request.complianceStatus());
        log.info("Updating compliance status for entity: {} to: {} tenant: {}", entityReference, complianceStatus, tenantId);
        return manageEntityStatusUseCase.updateComplianceStatus(
                tenantId, entityReference, complianceStatus, request.reason(), changedBy);
    }

    // ===== REQUEST RECORDS =====

    public record UpdateAccountStatusRequest(
            String accountStatus,
            String reason
    ) {}

    public record UpdateComplianceStatusRequest(
            String complianceStatus,
            String reason
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
