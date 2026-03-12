package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.adapter.rest.request.CreateFieldDefinitionRequest;
import com.ksa.financing.risk.adapter.rest.request.EvaluateEligibilityRequest;
import com.ksa.financing.risk.adapter.rest.request.SaveCreditScoringRequest;
import com.ksa.financing.risk.adapter.rest.request.UpdateFieldDefinitionRequest;
import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;
import com.ksa.financing.risk.domain.port.in.EvaluateEligibilityUseCase;
import com.ksa.financing.risk.domain.port.in.GetCreditScoringFieldsUseCase;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase.CreditScoringCriteriaCommand;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase.CreditScoringRuleCommand;
import com.ksa.financing.risk.domain.port.in.ManageFieldDefinitionsUseCase;
import com.ksa.financing.risk.domain.port.in.ManageFieldDefinitionsUseCase.CreateFieldDefinitionCommand;
import com.ksa.financing.risk.domain.port.in.ManageFieldDefinitionsUseCase.UpdateFieldDefinitionCommand;
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
import org.springframework.http.HttpStatus;
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
    private final ManageFieldDefinitionsUseCase manageFieldDefinitionsUseCase;
    private final EvaluateEligibilityUseCase evaluateEligibilityUseCase;

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

    // ==================== Field Definition CRUD (Super Admin Only) ====================

    @SecuredEndpoint(obj = "risk.credit-scoring.field-definitions", act = "read")
    @GetMapping("/field-definitions/all")
    @Operation(summary = "List all field definitions", description = "Returns all credit scoring field definitions (active and inactive) for admin management")
    public List<CreditScoringFieldDefinition> getAllFieldDefinitions(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Admin fetching all field definitions for tenant={}", tenantId);
        return manageFieldDefinitionsUseCase.getAllFieldDefinitions(tenantId);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring.field-definitions", act = "read")
    @GetMapping("/field-definitions/{id}")
    @Operation(summary = "Get field definition by ID", description = "Returns a single field definition by its ID")
    public CreditScoringFieldDefinition getFieldDefinitionById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Admin fetching field definition id={} for tenant={}", id, tenantId);
        return manageFieldDefinitionsUseCase.getFieldDefinitionById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring.field-definitions", act = "create")
    @PostMapping("/field-definitions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create field definition", description = "Creates a new credit scoring field definition")
    public CreditScoringFieldDefinition createFieldDefinition(
            @Valid @RequestBody CreateFieldDefinitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Admin creating field definition fieldKey={} for tenant={}", request.fieldKey(), tenantId);

        var command = new CreateFieldDefinitionCommand(
                request.fieldKey(), request.nameEn(), request.nameAr(),
                request.dataType(), request.active(), request.sortOrder()
        );

        return manageFieldDefinitionsUseCase.createFieldDefinition(tenantId, command);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring.field-definitions", act = "update")
    @PutMapping("/field-definitions/{id}")
    @Operation(summary = "Update field definition", description = "Updates an existing credit scoring field definition")
    public CreditScoringFieldDefinition updateFieldDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFieldDefinitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Admin updating field definition id={} for tenant={}", id, tenantId);

        var command = new UpdateFieldDefinitionCommand(
                request.fieldKey(), request.nameEn(), request.nameAr(),
                request.dataType(), request.active(), request.sortOrder()
        );

        return manageFieldDefinitionsUseCase.updateFieldDefinition(tenantId, id, command);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring.field-definitions", act = "delete")
    @DeleteMapping("/field-definitions/{id}")
    @Operation(summary = "Delete field definition", description = "Deletes a credit scoring field definition by ID")
    public ResponseEntity<Void> deleteFieldDefinition(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Admin deleting field definition id={} for tenant={}", id, tenantId);
        manageFieldDefinitionsUseCase.deleteFieldDefinition(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Decision Engine: Product Fields + Evaluate ====================

    @SecuredEndpoint(obj = "risk.credit-scoring", act = "read")
    @GetMapping("/products/{productId}/fields")
    @Operation(summary = "Get eligibility fields for a product",
            description = "Returns field definitions assigned to a product (for mobile form rendering). "
                    + "Falls back to all active definitions if no product-specific criteria exist.")
    public List<CreditScoringFieldDefinition> getProductFields(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Getting eligibility fields for product={} tenant={}", productId, tenantId);
        return evaluateEligibilityUseCase.getProductFields(tenantId, productId);
    }

    @SecuredEndpoint(obj = "risk.credit-scoring", act = "read")
    @PostMapping("/products/{productId}/evaluate")
    @Operation(summary = "Evaluate customer eligibility against product scoring rules",
            description = "Runs the decision engine: loads product criteria + rules, evaluates customer "
                    + "answers against each rule, returns score and eligibility decision.")
    public EligibilityEvaluationResult evaluateEligibility(
            @PathVariable UUID productId,
            @Valid @RequestBody EvaluateEligibilityRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Evaluating eligibility for product={} tenant={}", productId, tenantId);
        return evaluateEligibilityUseCase.evaluate(tenantId, productId, request.answers());
    }

    // ==================== Helpers ====================

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
