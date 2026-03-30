package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.model.parameter.ScoringThreshold;
import com.ksa.financing.risk.domain.port.in.ManageScoringThresholdUseCase;
import com.ksa.financing.risk.domain.port.in.ManageScoringThresholdUseCase.CreateThresholdCommand;
import com.ksa.financing.risk.domain.port.in.ManageScoringThresholdUseCase.UpdateThresholdCommand;
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
@RequestMapping("/api/v1/risk/thresholds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scoring Thresholds", description = "APIs for managing scoring thresholds")
public class ScoringThresholdController {

    private final ManageScoringThresholdUseCase manageScoringThresholdUseCase;

    @SecuredEndpoint(obj = "risk.thresholds", act = "create")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a scoring threshold")
    public ScoringThreshold create(
            @Valid @RequestBody CreateThresholdRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Creating scoring threshold for type: {} level: {} tenant: {}", request.riskType(), request.riskLevel(), tenantId);
        return manageScoringThresholdUseCase.create(tenantId, new CreateThresholdCommand(
                request.riskType(),
                request.riskLevel(),
                request.minScore(),
                request.maxScore(),
                request.descriptionEn(),
                request.descriptionAr()
        ));
    }

    @SecuredEndpoint(obj = "risk.thresholds", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update a scoring threshold")
    public ScoringThreshold update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateThresholdRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Updating scoring threshold: {} for tenant: {}", id, tenantId);
        return manageScoringThresholdUseCase.update(tenantId, id, new UpdateThresholdCommand(
                request.riskLevel(),
                request.minScore(),
                request.maxScore(),
                request.descriptionEn(),
                request.descriptionAr()
        ));
    }

    @SecuredEndpoint(obj = "risk.thresholds", act = "read")
    @GetMapping("/type/{riskType}")
    @Operation(summary = "Get scoring thresholds by risk type")
    public List<ScoringThreshold> getByRiskType(
            @PathVariable RiskType riskType,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageScoringThresholdUseCase.getByRiskType(tenantId, riskType);
    }

    @SecuredEndpoint(obj = "risk.thresholds", act = "read")
    @GetMapping("/type/{riskType}/active")
    @Operation(summary = "Get active scoring thresholds by risk type")
    public List<ScoringThreshold> getActiveByRiskType(
            @PathVariable RiskType riskType,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageScoringThresholdUseCase.getActiveByRiskType(tenantId, riskType);
    }

    @SecuredEndpoint(obj = "risk.thresholds", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a scoring threshold")
    public void deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Deactivating scoring threshold: {} for tenant: {}", id, tenantId);
        manageScoringThresholdUseCase.deactivate(tenantId, id);
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

    record CreateThresholdRequest(
            RiskType riskType,
            String riskLevel,
            BigDecimal minScore,
            BigDecimal maxScore,
            String descriptionEn,
            String descriptionAr
    ) {}

    record UpdateThresholdRequest(
            String riskLevel,
            BigDecimal minScore,
            BigDecimal maxScore,
            String descriptionEn,
            String descriptionAr
    ) {}
}
