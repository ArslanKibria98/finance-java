package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.adapter.rest.request.CreateFraudRuleRequest;
import com.ksa.financing.risk.adapter.rest.request.UpdateFraudRuleRequest;
import com.ksa.financing.risk.domain.model.FraudRule;
import com.ksa.financing.risk.domain.port.in.ManageFraudRulesUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
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
@RequestMapping("/api/v1/risk/fraud/rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Rules", description = "Configurable fraud detection rules management (30 rules from HLD)")
public class FraudRuleController {

    private final ManageFraudRulesUseCase manageFraudRulesUseCase;

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "read")
    @GetMapping
    @Operation(summary = "List all fraud rules", description = "Returns all fraud rules for the tenant, ordered by priority")
    public ResponseEntity<List<FraudRule>> listAll(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var rules = manageFraudRulesUseCase.listAll(tenantId);
        return ResponseEntity.ok(rules);
    }

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "read")
    @GetMapping("/category/{category}")
    @Operation(summary = "List fraud rules by category", description = "Filter by: LOCATION, DEVICE, GEOGRAPHIC_ACCESS, FINANCIAL, PAYMENT_CARD, TRANSACTION_MONITORING")
    public ResponseEntity<List<FraudRule>> listByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var rules = manageFraudRulesUseCase.listByCategory(tenantId, category);
        return ResponseEntity.ok(rules);
    }

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get fraud rule by ID")
    public ResponseEntity<FraudRule> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var rule = manageFraudRulesUseCase.getById(tenantId, id);
        return ResponseEntity.ok(rule);
    }

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "read")
    @GetMapping("/rule-id/{ruleId}")
    @Operation(summary = "Get fraud rule by rule ID (e.g., LOC_001, DEV_002)")
    public ResponseEntity<FraudRule> getByRuleId(
            @PathVariable String ruleId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var rule = manageFraudRulesUseCase.getByRuleId(tenantId, ruleId);
        return ResponseEntity.ok(rule);
    }

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "create")
    @PostMapping
    @Operation(summary = "Create fraud rule")
    public ResponseEntity<FraudRule> create(
            @Valid @RequestBody CreateFraudRuleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Creating fraud rule ruleId={} for tenant={}", request.ruleId(), tenantId);

        var rule = new FraudRule();
        rule.setRuleId(request.ruleId());
        rule.setScenarioName(request.scenarioName());
        rule.setScenarioNameAr(request.scenarioNameAr());
        rule.setCategory(request.category());
        rule.setDetectionLogic(request.detectionLogic());
        rule.setDefaultAction(request.defaultAction());
        rule.setBlockType(request.blockType());
        rule.setStatus(request.status() != null ? request.status() : "ACTIVE");
        rule.setParameters(request.parameters() != null ? request.parameters() : "[]");
        rule.setPriority(request.priority());

        var created = manageFraudRulesUseCase.create(tenantId, rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update fraud rule")
    public ResponseEntity<FraudRule> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFraudRuleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Updating fraud rule id={} for tenant={}", id, tenantId);

        var updates = new FraudRule();
        updates.setScenarioName(request.scenarioName());
        updates.setScenarioNameAr(request.scenarioNameAr());
        updates.setCategory(request.category());
        updates.setDetectionLogic(request.detectionLogic());
        updates.setDefaultAction(request.defaultAction());
        updates.setBlockType(request.blockType());
        updates.setStatus(request.status());
        updates.setParameters(request.parameters());
        updates.setPriority(request.priority());

        var updated = manageFraudRulesUseCase.update(tenantId, id, updates);
        return ResponseEntity.ok(updated);
    }

    @SecuredEndpoint(obj = "risk.fraud-rules", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete fraud rule")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Deleting fraud rule id={} for tenant={}", id, tenantId);

        manageFraudRulesUseCase.delete(tenantId, id);
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
