package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.CreateDunningPolicyRequest;
import com.ksa.financing.collections.adapter.rest.request.UpdateDunningPolicyRequest;
import com.ksa.financing.collections.adapter.rest.response.DelinquencySimulationResponse;
import com.ksa.financing.collections.adapter.rest.response.DunningPolicyResponse;
import com.ksa.financing.collections.application.service.DunningPolicyResolver;
import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningThresholds;
import com.ksa.financing.collections.domain.model.LateFeeConfig;
import com.ksa.financing.collections.domain.model.LateFeeType;
import com.ksa.financing.collections.domain.model.SimahReportingConfig;
import com.ksa.financing.collections.domain.port.in.ManageDunningPolicyUseCase;
import com.ksa.financing.collections.domain.port.in.ManageDunningPolicyUseCase.CreatePolicyCommand;
import com.ksa.financing.collections.domain.port.in.ManageDunningPolicyUseCase.UpdatePolicyCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dunning-policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Dunning Policies",
        description = "Admin-configurable, product-wise delinquency rules. Drives the dynamic dunning flow.")
public class AdminDunningPolicyController {

    private final ManageDunningPolicyUseCase manageUseCase;
    private final DunningPolicyResolver resolver;

    @PostMapping
    @Operation(summary = "Create a dunning policy (tenant-default or product-specific)")
    @SecuredEndpoint(obj = "dunning.policies", act = "create")
    public ResponseEntity<DunningPolicyResponse> create(
            @Valid @RequestBody CreateDunningPolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new CreatePolicyCommand(
                tenantId,
                request.policyName(),
                request.productCode(),
                request.description(),
                request.defaultPolicy(),
                buildThresholds(request),
                buildLateFee(request),
                buildSimah(request),
                request.stageActions(),
                request.autoAssignAgent(),
                request.agentAssignmentDpd(),
                request.walletFreezeDpd(),
                userId);

        var policy = manageUseCase.createPolicy(command);
        log.info("Dunning policy created: id={} product={} tenant={}",
                policy.getId(), policy.getProductCode(), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(policy));
    }

    @PutMapping("/{policyId}")
    @Operation(summary = "Update a dunning policy (partial — only non-null fields)")
    @SecuredEndpoint(obj = "dunning.policies", act = "update")
    public ResponseEntity<DunningPolicyResponse> update(
            @PathVariable UUID policyId,
            @Valid @RequestBody UpdateDunningPolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new UpdatePolicyCommand(
                tenantId,
                policyId,
                request.policyName(),
                request.description(),
                request.productCode(),
                maybeThresholds(request),
                maybeLateFee(request),
                maybeSimah(request),
                request.stageActions(),
                request.autoAssignAgent(),
                request.agentAssignmentDpd(),
                request.walletFreezeDpd(),
                userId);

        return ResponseEntity.ok(toResponse(manageUseCase.updatePolicy(command)));
    }

    @PostMapping("/{policyId}/activate")
    @Operation(summary = "Activate a dunning policy")
    @SecuredEndpoint(obj = "dunning.policies", act = "update")
    public ResponseEntity<DunningPolicyResponse> activate(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(toResponse(manageUseCase.activatePolicy(tenantId, policyId, userId)));
    }

    @PostMapping("/{policyId}/deactivate")
    @Operation(summary = "Deactivate a dunning policy")
    @SecuredEndpoint(obj = "dunning.policies", act = "update")
    public ResponseEntity<DunningPolicyResponse> deactivate(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(toResponse(manageUseCase.deactivatePolicy(tenantId, policyId, userId)));
    }

    @PostMapping("/{policyId}/mark-default")
    @Operation(summary = "Mark this policy as the tenant-wide default (clears other defaults)")
    @SecuredEndpoint(obj = "dunning.policies", act = "update")
    public ResponseEntity<DunningPolicyResponse> markDefault(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(toResponse(manageUseCase.markAsDefault(tenantId, policyId, userId)));
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "Get a dunning policy")
    @SecuredEndpoint(obj = "dunning.policies", act = "read")
    public ResponseEntity<DunningPolicyResponse> get(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(manageUseCase.getPolicy(tenantId, policyId)));
    }

    @GetMapping
    @Operation(summary = "List dunning policies for the current tenant")
    @SecuredEndpoint(obj = "dunning.policies", act = "read")
    public ResponseEntity<List<DunningPolicyResponse>> list(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(
                manageUseCase.listPolicies(tenantId, activeOnly).stream()
                        .map(this::toResponse)
                        .toList());
    }

    @DeleteMapping("/{policyId}")
    @Operation(summary = "Delete a dunning policy")
    @SecuredEndpoint(obj = "dunning.policies", act = "delete")
    public ResponseEntity<Void> delete(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        manageUseCase.deletePolicy(tenantId, policyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/simulate")
    @Operation(summary = "Simulate how the resolved policy handles a given DPD + product")
    @SecuredEndpoint(obj = "dunning.policies", act = "read")
    public ResponseEntity<DelinquencySimulationResponse> simulate(
            @RequestParam(required = false) String productCode,
            @RequestParam int dpd,
            @RequestParam(required = false, defaultValue = "1000") BigDecimal outstanding,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var resolved = resolver.resolve(tenantId, productCode);

        var stage = resolved.thresholds().stageFor(dpd);
        var lateFee = resolved.lateFee().computeFee(outstanding, dpd);

        boolean simahReport = resolved.simah().shouldReport(dpd);
        boolean simahDefault = resolved.simah().shouldMarkDefault(dpd);
        boolean assignAgent = resolved.hasPersistedPolicy()
                && resolved.policy().shouldAssignAgent(dpd);
        boolean freezeWallet = resolved.hasPersistedPolicy()
                && resolved.policy().shouldFreezeWallet(dpd);

        return ResponseEntity.ok(new DelinquencySimulationResponse(
                productCode, dpd, stage, resolved.source().name(),
                simahReport, simahDefault, assignAgent, freezeWallet,
                lateFee, resolved.lateFee().charityFundAccount()));
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private DunningThresholds buildThresholds(CreateDunningPolicyRequest r) {
        return new DunningThresholds(
                defaultInt(r.preDueDaysBefore(), 3),
                defaultInt(r.gracePeriodDays(), 10),
                defaultInt(r.softCollectionDpd(), 11),
                defaultInt(r.hardCollectionDpd(), 31),
                defaultInt(r.legalDpd(), 91),
                defaultInt(r.writeOffDpd(), 181));
    }

    private DunningThresholds maybeThresholds(UpdateDunningPolicyRequest r) {
        if (r.preDueDaysBefore() == null && r.gracePeriodDays() == null
                && r.softCollectionDpd() == null && r.hardCollectionDpd() == null
                && r.legalDpd() == null && r.writeOffDpd() == null) {
            return null;
        }
        return new DunningThresholds(
                defaultInt(r.preDueDaysBefore(), 3),
                defaultInt(r.gracePeriodDays(), 10),
                defaultInt(r.softCollectionDpd(), 11),
                defaultInt(r.hardCollectionDpd(), 31),
                defaultInt(r.legalDpd(), 91),
                defaultInt(r.writeOffDpd(), 181));
    }

    private LateFeeConfig buildLateFee(CreateDunningPolicyRequest r) {
        if (r.lateFeeEnabled() == null || !r.lateFeeEnabled()) {
            return LateFeeConfig.disabled();
        }
        return new LateFeeConfig(
                true,
                LateFeeType.valueOf(r.lateFeeType()),
                r.lateFeeAmount(),
                r.lateFeePercentage(),
                defaultInt(r.lateFeeMinDpd(), 1),
                r.lateFeeMaxAmount(),
                r.charityFundAccount());
    }

    private LateFeeConfig maybeLateFee(UpdateDunningPolicyRequest r) {
        if (r.lateFeeEnabled() == null) return null;
        if (!r.lateFeeEnabled()) return LateFeeConfig.disabled();
        return new LateFeeConfig(
                true,
                r.lateFeeType() != null ? LateFeeType.valueOf(r.lateFeeType()) : LateFeeType.PERCENTAGE,
                r.lateFeeAmount(),
                r.lateFeePercentage(),
                defaultInt(r.lateFeeMinDpd(), 1),
                r.lateFeeMaxAmount(),
                r.charityFundAccount());
    }

    private SimahReportingConfig buildSimah(CreateDunningPolicyRequest r) {
        boolean enabled = r.simahReportEnabled() == null || r.simahReportEnabled();
        return new SimahReportingConfig(
                enabled,
                defaultInt(r.simahReportDpd(), 60),
                defaultInt(r.simahDefaultStatusDpd(), 90));
    }

    private SimahReportingConfig maybeSimah(UpdateDunningPolicyRequest r) {
        if (r.simahReportEnabled() == null && r.simahReportDpd() == null
                && r.simahDefaultStatusDpd() == null) {
            return null;
        }
        boolean enabled = r.simahReportEnabled() == null || r.simahReportEnabled();
        return new SimahReportingConfig(
                enabled,
                defaultInt(r.simahReportDpd(), 60),
                defaultInt(r.simahDefaultStatusDpd(), 90));
    }

    private int defaultInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(
                    "COMMON.AUTH.ACCESS_DENIED", "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private DunningPolicyResponse toResponse(DunningPolicy p) {
        var lf = p.getLateFee();
        var s = p.getSimah();
        var t = p.getThresholds();
        return new DunningPolicyResponse(
                p.getId().getValue(), p.getTenantId(), p.getPolicyName(), p.getProductCode(),
                p.getDescription(), p.isActive(), p.isDefaultPolicy(),
                t.preDueDaysBefore(), t.gracePeriodDays(),
                t.softCollectionDpd(), t.hardCollectionDpd(), t.legalDpd(), t.writeOffDpd(),
                lf.enabled(), lf.type() != null ? lf.type().name() : null,
                lf.flatAmount(), lf.percentage(), lf.minDpd(), lf.maxAmount(), lf.charityFundAccount(),
                s.enabled(), s.reportDpd(), s.defaultStatusDpd(),
                p.isAutoAssignAgent(), p.getAgentAssignmentDpd(), p.getWalletFreezeDpd(),
                p.getStageActions(),
                p.getVersion(), p.getCreatedAt(), p.getUpdatedAt(),
                p.getCreatedBy(), p.getUpdatedBy());
    }
}
