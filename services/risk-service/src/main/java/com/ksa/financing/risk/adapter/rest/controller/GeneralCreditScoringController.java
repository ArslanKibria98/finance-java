package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.risk.adapter.rest.request.EvaluateGeneralScoringRequest;
import com.ksa.financing.risk.adapter.rest.request.SaveGeneralScoringRequest;
import com.ksa.financing.risk.adapter.rest.request.SaveSingleGeneralCriteriaRequest;
import com.ksa.financing.risk.adapter.rest.request.UpdateGeneralScoringConfigRequest;
import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreSnapshot;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;
import com.ksa.financing.risk.domain.model.credit.GeneralScoringConfig;
import com.ksa.financing.risk.domain.port.in.EvaluateGeneralScoringUseCase;
import com.ksa.financing.risk.domain.port.in.EvaluateGeneralScoringUseCase.EvaluateInput;
import com.ksa.financing.risk.domain.port.in.ManageGeneralScoringUseCase;
import com.ksa.financing.risk.domain.port.in.ManageGeneralScoringUseCase.GeneralCriteriaCommand;
import com.ksa.financing.risk.domain.port.in.ManageGeneralScoringUseCase.GeneralRuleCommand;
import com.ksa.financing.risk.domain.port.in.ManageGeneralScoringUseCase.UpdateGeneralConfigCommand;
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
import java.util.Map;
import java.util.UUID;

/**
 * REST API for managing the GENERAL (product-agnostic, onboarding) credit
 * scoring configuration. Sibling to {@link CreditScoringController} which
 * manages per-product criteria for loan applications.
 *
 * General scoring fields = onboarding-relevant only (gender, nationality,
 * occupation, source_of_funds, source_of_wealth, net_worth_range, salary,
 * employment_type, etc.). SIMAH and product-specific fields stay in the
 * product scoring tables, not here.
 */
@RestController
@RequestMapping("/api/v1/risk/credit-scoring/general")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "General Credit Scoring",
        description = "Product-agnostic credit scoring used during customer onboarding")
public class GeneralCreditScoringController {

    private final ManageGeneralScoringUseCase manageUseCase;
    private final EvaluateGeneralScoringUseCase evaluateUseCase;

    // -------------------- Criteria CRUD --------------------

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "read")
    @GetMapping("/criteria")
    @Operation(summary = "List general scoring criteria",
            description = "Returns the tenant's general (onboarding) credit scoring criteria with rules")
    public List<CreditScoringCriteria> listCriteria(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Listing general scoring criteria for tenant={}", tenantId);
        return manageUseCase.getCriteria(tenantId);
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "manage")
    @PutMapping("/criteria")
    @Operation(summary = "Save general scoring criteria",
            description = "Replaces ALL existing general scoring criteria with the provided set")
    public ResponseEntity<Void> saveCriteria(
            @Valid @RequestBody SaveGeneralScoringRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Saving general scoring criteria for tenant={} count={}",
                tenantId, request.criteria().size());

        var commands = request.criteria().stream()
                .map(c -> new GeneralCriteriaCommand(
                        c.fieldDefinitionId(),
                        c.customName(),
                        c.custom(),
                        c.enabled(),
                        c.sortOrder(),
                        c.rules() == null ? List.of() : c.rules().stream()
                                .map(r -> new GeneralRuleCommand(
                                        r.operator(), r.value(), r.weight(), r.percentage()))
                                .toList()
                ))
                .toList();

        manageUseCase.saveCriteria(tenantId, commands);
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "manage")
    @DeleteMapping("/criteria")
    @Operation(summary = "Delete ALL general scoring criteria for the tenant")
    public ResponseEntity<Void> deleteAllCriteria(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Deleting all general scoring criteria for tenant={}", tenantId);
        manageUseCase.deleteAllCriteria(tenantId);
        return ResponseEntity.noContent().build();
    }

    // -------------------- Single-criterion CRUD (tenant-level admin UI) --------------------

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "read")
    @GetMapping("/criteria/{id}")
    @Operation(summary = "Get one general criterion by ID")
    public CreditScoringCriteria getCriteriaById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageUseCase.getCriteriaById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "manage")
    @PostMapping("/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add ONE general scoring criterion + rules",
            description = "Tenant-level admin add. For bulk replace, use PUT /criteria.")
    public CreditScoringCriteria createCriteria(
            @Valid @RequestBody SaveSingleGeneralCriteriaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Creating one general criterion for tenant={} fieldDef={}",
                tenantId, request.fieldDefinitionId());
        return manageUseCase.createCriteria(tenantId, toCommand(request));
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "manage")
    @PutMapping("/criteria/{id}")
    @Operation(summary = "Update ONE general scoring criterion + rules",
            description = "Replaces the criterion and ALL its rules. Use PUT /criteria for bulk update.")
    public CreditScoringCriteria updateCriteria(
            @PathVariable UUID id,
            @Valid @RequestBody SaveSingleGeneralCriteriaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Updating general criterion id={} for tenant={}", id, tenantId);
        return manageUseCase.updateCriteria(tenantId, id, toCommand(request));
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "manage")
    @DeleteMapping("/criteria/{id}")
    @Operation(summary = "Delete ONE general scoring criterion (rules cascade)")
    public ResponseEntity<Void> deleteSingleCriteria(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Deleting general criterion id={} for tenant={}", id, tenantId);
        manageUseCase.deleteCriteria(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private static GeneralCriteriaCommand toCommand(SaveSingleGeneralCriteriaRequest r) {
        return new GeneralCriteriaCommand(
                r.fieldDefinitionId(),
                r.customName(),
                r.custom(),
                r.enabled(),
                r.sortOrder(),
                r.rules() == null ? List.of() : r.rules().stream()
                        .map(rl -> new GeneralRuleCommand(
                                rl.operator(), rl.value(), rl.weight(), rl.percentage()))
                        .toList()
        );
    }

    // -------------------- Config --------------------

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "read")
    @GetMapping("/config")
    @Operation(summary = "Get general scoring thresholds (Green/Amber/Red)")
    public GeneralScoringConfig getConfig(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageUseCase.getConfig(tenantId);
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "manage")
    @PutMapping("/config")
    @Operation(summary = "Update general scoring thresholds")
    public GeneralScoringConfig updateConfig(
            @Valid @RequestBody UpdateGeneralScoringConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageUseCase.updateConfig(tenantId,
                new UpdateGeneralConfigCommand(
                        request.minPassPercentage(),
                        request.greenThreshold(),
                        request.amberThreshold(),
                        request.enabled()
                ));
    }

    // -------------------- Evaluate / preview --------------------

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "evaluate")
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate general scoring (preview / admin testing)",
            description = "Runs the scoring engine against general criteria. Set persistSnapshot=true to also store the result against a customer.")
    public EligibilityEvaluationResult evaluate(
            @Valid @RequestBody EvaluateGeneralScoringRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Evaluating general scoring tenant={} customer={} stage={}",
                tenantId, request.customerId(), request.stage());

        return evaluateUseCase.evaluate(new EvaluateInput(
                tenantId,
                request.customerId(),
                request.workflowId(),
                request.stage(),
                request.answers() == null ? Map.of() : request.answers(),
                request.shouldPersist()
        ));
    }

    // -------------------- Customer scoring history --------------------

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "read")
    @GetMapping("/customers/{customerId}/current")
    @Operation(summary = "Current cached score for a customer")
    public ResponseEntity<CustomerCreditScoreCurrent> getCurrentScore(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return evaluateUseCase.getCurrentScore(tenantId, customerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @SecuredEndpoint(obj = "risk.general-credit-scoring", act = "read")
    @GetMapping("/customers/{customerId}/history")
    @Operation(summary = "Full snapshot history for a customer (one row per stage)")
    public List<CustomerCreditScoreSnapshot> getSnapshotHistory(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return evaluateUseCase.getSnapshotHistory(tenantId, customerId);
    }

    // -------------------- helpers --------------------

    private UUID extractTenantId(Jwt jwt) {
        var claim = jwt.getClaimAsString("tenant_id");
        if (claim == null || claim.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(claim);
    }
}
