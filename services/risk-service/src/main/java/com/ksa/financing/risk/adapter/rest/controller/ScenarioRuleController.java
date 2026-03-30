package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.model.scenario.ThirdPartyCheckType;
import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;
import com.ksa.financing.risk.domain.port.in.ManageScenarioRuleUseCase;
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
@RequestMapping("/api/v1/risk/scenarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scenario Rules", description = "APIs for managing scenario configuration rules")
public class ScenarioRuleController {

    private final ManageScenarioRuleUseCase manageScenarioRuleUseCase;

    @SecuredEndpoint(obj = "risk.scenarios", act = "create")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new scenario rule")
    public ScenarioRule create(
            @Valid @RequestBody CreateScenarioRuleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Creating scenario rule: {} tenant: {}", request.scenarioName(), tenantId);
        var command = new ManageScenarioRuleUseCase.CreateScenarioRuleCommand(
                request.scenarioName(),
                request.scenarioNameAr(),
                request.triggerRiskStatus(),
                request.triggerPepFlag(),
                request.triggerThirdPartyCheckType() != null
                        ? ThirdPartyCheckType.valueOf(request.triggerThirdPartyCheckType()) : null,
                request.triggerThirdPartyResult(),
                request.resultingAccountStatus() != null
                        ? AccountStatus.valueOf(request.resultingAccountStatus()) : null,
                request.resultingComplianceStatus() != null
                        ? ComplianceStatus.valueOf(request.resultingComplianceStatus()) : null,
                request.requiresManualReview(),
                request.notifyRole(),
                request.priority(),
                request.slaDurationHours()
        );
        return manageScenarioRuleUseCase.create(tenantId, command);
    }

    @SecuredEndpoint(obj = "risk.scenarios", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing scenario rule")
    public ScenarioRule update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScenarioRuleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Updating scenario rule: {} tenant: {}", id, tenantId);
        var command = new ManageScenarioRuleUseCase.UpdateScenarioRuleCommand(
                request.scenarioName(),
                request.scenarioNameAr(),
                request.triggerRiskStatus(),
                request.triggerPepFlag(),
                request.triggerThirdPartyCheckType() != null
                        ? ThirdPartyCheckType.valueOf(request.triggerThirdPartyCheckType()) : null,
                request.triggerThirdPartyResult(),
                request.resultingAccountStatus() != null
                        ? AccountStatus.valueOf(request.resultingAccountStatus()) : null,
                request.resultingComplianceStatus() != null
                        ? ComplianceStatus.valueOf(request.resultingComplianceStatus()) : null,
                request.requiresManualReview(),
                request.notifyRole(),
                request.priority(),
                request.slaDurationHours()
        );
        return manageScenarioRuleUseCase.update(tenantId, id, command);
    }

    @SecuredEndpoint(obj = "risk.scenarios", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get a scenario rule by ID")
    public ScenarioRule getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageScenarioRuleUseCase.getById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.scenarios", act = "read")
    @GetMapping
    @Operation(summary = "Get all scenario rules")
    public List<ScenarioRule> getAll(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageScenarioRuleUseCase.getAll(tenantId);
    }

    @SecuredEndpoint(obj = "risk.scenarios", act = "read")
    @GetMapping("/active")
    @Operation(summary = "Get all active scenario rules")
    public List<ScenarioRule> getActive(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageScenarioRuleUseCase.getActive(tenantId);
    }

    @SecuredEndpoint(obj = "risk.scenarios", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a scenario rule")
    public void deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Deactivating scenario rule: {} tenant: {}", id, tenantId);
        manageScenarioRuleUseCase.deactivate(tenantId, id);
    }

    // ===== REQUEST RECORDS =====

    public record CreateScenarioRuleRequest(
            String scenarioName,
            String scenarioNameAr,
            String triggerRiskStatus,
            Boolean triggerPepFlag,
            String triggerThirdPartyCheckType,
            String triggerThirdPartyResult,
            String resultingAccountStatus,
            String resultingComplianceStatus,
            boolean requiresManualReview,
            String notifyRole,
            int priority,
            int slaDurationHours
    ) {}

    public record UpdateScenarioRuleRequest(
            String scenarioName,
            String scenarioNameAr,
            String triggerRiskStatus,
            Boolean triggerPepFlag,
            String triggerThirdPartyCheckType,
            String triggerThirdPartyResult,
            String resultingAccountStatus,
            String resultingComplianceStatus,
            Boolean requiresManualReview,
            String notifyRole,
            Integer priority,
            Integer slaDurationHours
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
