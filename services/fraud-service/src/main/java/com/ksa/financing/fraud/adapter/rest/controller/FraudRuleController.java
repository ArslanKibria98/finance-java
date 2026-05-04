package com.ksa.financing.fraud.adapter.rest.controller;

import com.ksa.financing.fraud.application.dto.FraudRuleConfigDto;
import com.ksa.financing.fraud.application.mapper.FraudEventMapper;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.port.in.ManageFraudRulesUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud/rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Rules", description = "Manage configurable fraud detection rules per tenant")
public class FraudRuleController {

    private final ManageFraudRulesUseCase manageFraudRulesUseCase;

    @SecuredEndpoint(obj = "fraud.rules", act = "read")
    @GetMapping
    @Operation(summary = "List all fraud rules (paginated)", description = "Returns a paginated list of fraud rules for the tenant")
    public PageResponse<FraudRuleConfigDto> getAllRules(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageFraudRulesUseCase.getAllRules(tenantId, pageQuery)
                .map(FraudEventMapper::toRuleConfigDto);
    }

    @SecuredEndpoint(obj = "fraud.rules", act = "read")
    @GetMapping("/active")
    @Operation(summary = "List active fraud rules (paginated)", description = "Returns a paginated list of only active fraud rules")
    public PageResponse<FraudRuleConfigDto> getActiveRules(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageFraudRulesUseCase.getActiveRules(tenantId, pageQuery)
                .map(FraudEventMapper::toRuleConfigDto);
    }

    @SecuredEndpoint(obj = "fraud.rules", act = "read")
    @GetMapping("/{ruleId}")
    @Operation(summary = "Get fraud rule by ID", description = "Returns a specific fraud rule")
    public FraudRuleConfigDto getRule(
            @PathVariable String ruleId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var rule = manageFraudRulesUseCase.getRule(tenantId, parseRuleId(ruleId));
        return FraudEventMapper.toRuleConfigDto(rule);
    }

    @SecuredEndpoint(obj = "fraud.rules", act = "update")
    @PutMapping("/{ruleId}/enable")
    @Operation(summary = "Enable fraud rule", description = "Activates a fraud rule for this tenant")
    public ResponseEntity<Void> enableRule(
            @PathVariable String ruleId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageFraudRulesUseCase.enableRule(tenantId, parseRuleId(ruleId));
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "fraud.rules", act = "update")
    @PutMapping("/{ruleId}/disable")
    @Operation(summary = "Disable fraud rule", description = "Disables a fraud rule for this tenant")
    public ResponseEntity<Void> disableRule(
            @PathVariable String ruleId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageFraudRulesUseCase.disableRule(tenantId, parseRuleId(ruleId));
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "fraud.rules", act = "update")
    @PutMapping("/{ruleId}/parameters")
    @Operation(summary = "Update rule parameters", description = "Updates configurable thresholds for a fraud rule")
    public ResponseEntity<Void> updateParameters(
            @PathVariable String ruleId,
            @RequestBody String parametersJson,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageFraudRulesUseCase.updateRuleParameters(tenantId, parseRuleId(ruleId), parametersJson);
        return ResponseEntity.ok().build();
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

    private FraudRuleId parseRuleId(String ruleId) {
        try {
            return FraudRuleId.valueOf(ruleId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.BAD_REQUEST,
                    "Invalid fraud rule ID: " + ruleId);
        }
    }
}
