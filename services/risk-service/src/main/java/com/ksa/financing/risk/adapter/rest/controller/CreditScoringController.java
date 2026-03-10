package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.adapter.rest.request.SaveCreditScoringRequest;
import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.port.in.GetCreditScoringFieldsUseCase;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase.CreditScoringCriteriaCommand;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase.CreditScoringRuleCommand;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/credit-scoring")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Credit Scoring", description = "Credit scoring field definitions and product criteria management")
public class CreditScoringController {

    private final GetCreditScoringFieldsUseCase getCreditScoringFieldsUseCase;
    private final ManageCreditScoringUseCase manageCreditScoringUseCase;

    @SecuredEndpoint(obj = "risk.credit-scoring", act = "read")
    @GetMapping("/field-definitions")
    @Operation(summary = "List active field definitions", description = "Returns all active credit scoring field definitions for the tenant")
    public List<CreditScoringFieldDefinition> getFieldDefinitions(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Fetching credit scoring field definitions for tenant={}", tenantId);
        return getCreditScoringFieldsUseCase.getActiveFieldDefinitions(tenantId);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring", act = "read")
    @GetMapping("/products/{productId}/criteria")
    @Operation(summary = "Get product criteria", description = "Returns saved credit scoring criteria and rules for a product")
    public List<CreditScoringCriteria> getCriteria(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Fetching credit scoring criteria for product={} tenant={}", productId, tenantId);
        return manageCreditScoringUseCase.getCriteriaByProduct(tenantId, productId);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring", act = "create")
    @PutMapping("/products/{productId}/criteria")
    @Operation(summary = "Save product criteria", description = "Replaces all credit scoring criteria and rules for a product")
    public ResponseEntity<Void> saveCriteria(
            @PathVariable UUID productId,
            @Valid @RequestBody SaveCreditScoringRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Saving credit scoring criteria for product={} tenant={}", productId, tenantId);

        var commands = request.criteria().stream()
                .map(c -> new CreditScoringCriteriaCommand(
                        c.fieldDefinitionId(),
                        c.customName(),
                        c.custom(),
                        c.enabled(),
                        c.sortOrder(),
                        c.rules() == null ? List.of() : c.rules().stream()
                                .map(r -> new CreditScoringRuleCommand(
                                        r.operator(), r.value(), r.weight(), r.percentage()))
                                .toList()
                ))
                .toList();

        manageCreditScoringUseCase.saveCriteria(tenantId, productId, commands);
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "risk.credit-scoring", act = "delete")
    @DeleteMapping("/products/{productId}/criteria")
    @Operation(summary = "Delete product criteria", description = "Deletes all credit scoring criteria and rules for a product")
    public ResponseEntity<Void> deleteCriteria(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Deleting credit scoring criteria for product={} tenant={}", productId, tenantId);
        manageCreditScoringUseCase.deleteCriteriaByProduct(tenantId, productId);
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
}
