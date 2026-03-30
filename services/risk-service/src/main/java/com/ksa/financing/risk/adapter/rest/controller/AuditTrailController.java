package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;
import com.ksa.financing.risk.domain.port.in.QueryAuditTrailUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Trail", description = "Immutable audit trail queries (read-only)")
public class AuditTrailController {

    private final QueryAuditTrailUseCase queryAuditTrailUseCase;

    @SecuredEndpoint(obj = "risk.audit", act = "read")
    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit trail for a specific entity")
    public List<AuditEntry> getByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return queryAuditTrailUseCase.getByEntity(tenantId, AuditEntityType.valueOf(entityType), entityId);
    }

    @SecuredEndpoint(obj = "risk.audit", act = "read")
    @GetMapping("/actor/{actorId}")
    @Operation(summary = "Get audit trail for a specific actor within date range")
    public List<AuditEntry> getByActor(
            @PathVariable UUID actorId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return queryAuditTrailUseCase.getByActor(tenantId, actorId, from, to);
    }

    @SecuredEndpoint(obj = "risk.audit", act = "read")
    @GetMapping("/date-range")
    @Operation(summary = "Get audit trail within date range")
    public List<AuditEntry> getByDateRange(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return queryAuditTrailUseCase.getByDateRange(tenantId, from, to);
    }

    @SecuredEndpoint(obj = "risk.audit", act = "read")
    @GetMapping("/correlation/{correlationId}")
    @Operation(summary = "Get audit trail by correlation ID")
    public List<AuditEntry> getByCorrelationId(
            @PathVariable String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return queryAuditTrailUseCase.getByCorrelationId(tenantId, correlationId);
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
