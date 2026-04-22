package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.UpsertDelinquencyRuleRequest;
import com.ksa.financing.collections.adapter.rest.response.DelinquencyRuleResponse;
import com.ksa.financing.collections.adapter.rest.response.EarlySettlementConfigResponse;
import com.ksa.financing.collections.application.service.DelinquencyRulesResolver;
import com.ksa.financing.collections.application.service.EarlySettlementMatcher;
import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.EarlySettlementConfig;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.collections.domain.port.in.ManageDelinquencyRuleUseCase;
import com.ksa.financing.collections.domain.port.in.ManageDelinquencyRuleUseCase.ConfigItem;
import com.ksa.financing.collections.domain.port.in.ManageDelinquencyRuleUseCase.ConfigKind;
import com.ksa.financing.collections.domain.port.in.ManageDelinquencyRuleUseCase.UpsertRuleCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/delinquency-rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Delinquency Rules",
        description = "Per-product delinquency configuration: 6 lifecycle stages (Early Settlement, Due Loan, Late Payment, Write-offs, Non-Performing Loan, Broken Promises). One atomic PUT carries the rule and its early-settlement configs.")
public class DelinquencyRuleController {

    private final ManageDelinquencyRuleUseCase manageUseCase;
    private final DelinquencyRulesResolver rulesResolver;
    private final EarlySettlementMatcher matcher;
    private final RepaymentScheduleRepository scheduleRepository;

    @PutMapping
    @Operation(summary = "Create or update a delinquency rule (atomic: rule + early-settlement configs)")
    @SecuredEndpoint(obj = "delinquency.rules", act = "manage")
    public ResponseEntity<DelinquencyRuleResponse> upsert(
            @Valid @RequestBody UpsertDelinquencyRuleRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var type = DelinquencyType.fromCode(req.delinquencyType());

        var cmd = new UpsertRuleCommand(
                tenantId,
                req.productId(),
                type,
                Boolean.TRUE.equals(req.isPercentage()),
                nullToZero(req.penaltyPercentage()),
                nullToZero(req.penaltyAmount()),
                nullToZero(req.fromDay()),
                nullToZero(req.tillDay()),
                req.penaltyType() != null ? req.penaltyType() : (type == DelinquencyType.BROKEN_PROMISES ? 0 : 1),
                nullToZero(req.promisesPerYear()),
                nullToZero(req.promisesPerLoan()),
                Boolean.TRUE.equals(req.isCustom()),
                req.charityFundAccount(),
                toConfigItems(req.configs()));

        var rule = manageUseCase.upsertRule(cmd);
        log.info("Delinquency rule upserted: id={} tenant={} product={} type={} configs={}",
                rule.getId().getValue(), tenantId, rule.getProductId(), rule.getDelinquencyType(),
                rule.listEarlySettlementConfigs().size());
        return ResponseEntity.status(HttpStatus.OK).body(toResponse(rule));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Soft-delete a delinquency rule (record_state=0)")
    @SecuredEndpoint(obj = "delinquency.rules", act = "delete")
    public ResponseEntity<Void> softDelete(
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        manageUseCase.softDeleteRule(tenantId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{ruleId}")
    @Operation(summary = "Get a delinquency rule by id")
    @SecuredEndpoint(obj = "delinquency.rules", act = "read")
    public ResponseEntity<DelinquencyRuleResponse> get(
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(manageUseCase.getById(tenantId, ruleId)));
    }

    @GetMapping
    @Operation(summary = "List all delinquency rules for a product (all 6 stages)")
    @SecuredEndpoint(obj = "delinquency.rules", act = "read")
    public ResponseEntity<List<DelinquencyRuleResponse>> listForProduct(
            @RequestParam UUID productId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(
                manageUseCase.listForProduct(tenantId, productId).stream()
                        .map(this::toResponse)
                        .toList());
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Strict per-loan early-settlement eligibility: true only if the product has an "
            + "active EARLY_SETTLEMENT rule AND the loan's schedule has at least one unpaid installment "
            + "currently inside the rule's DPD window (and matching an EarlySettlementConfig row for custom mode).")
    // Customer-facing: borrowers ask \"can I settle loan X early right now?\" — grants `customer`
    // via loans.early-settlement:read rather than the admin-only delinquency.rules:read.
    @SecuredEndpoint(obj = "loans.early-settlement", act = "read")
    public ResponseEntity<java.util.Map<UUID, Boolean>> earlySettlementEligibility(
            @RequestParam("loanIds") List<UUID> loanIds,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate today = LocalDate.now();
        var out = new java.util.LinkedHashMap<UUID, Boolean>();

        for (UUID loanId : loanIds) {
            out.put(loanId, computeStrictEligibility(tenantId, loanId, today));
        }
        return ResponseEntity.ok(out);
    }

    private boolean computeStrictEligibility(UUID tenantId, UUID loanId, LocalDate today) {
        var scheduleOpt = scheduleRepository.findActiveByLoanId(tenantId, loanId);
        if (scheduleOpt.isEmpty()) return false;
        var schedule = scheduleOpt.get();
        if (schedule.getProductId() == null) return false;

        DelinquencyRule rule = rulesResolver.rule(tenantId, schedule.getProductId(), DelinquencyType.EARLY_SETTLEMENT)
                .orElse(null);
        if (rule == null) return false;

        return matcher.anyInstallmentEligible(rule, schedule, today);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static int nullToZero(Integer v) {
        return v != null ? v : 0;
    }

    private static List<ConfigItem> toConfigItems(List<UpsertDelinquencyRuleRequest.ConfigItem> src) {
        if (src == null) return List.of();
        return src.stream()
                .map(i -> new ConfigItem(
                        ConfigKind.valueOf(i.kind().name()),
                        Boolean.TRUE.equals(i.isPercentage()),
                        nullToZero(i.discountPercentage()),
                        nullToZero(i.discountAmount()),
                        nullToZero(i.fromDay()),
                        nullToZero(i.tillDay()),
                        i.invoiceOrder(),
                        i.rangeNo(),
                        i.minInvoiceOrder(),
                        i.maxInvoiceOrder()))
                .toList();
    }

    private DelinquencyRuleResponse toResponse(DelinquencyRule r) {
        var configs = r.listEarlySettlementConfigs().stream()
                .map(this::toResponse)
                .toList();
        return new DelinquencyRuleResponse(
                r.getId().getValue(),
                r.getTenantId(),
                r.getProductId(),
                r.getDelinquencyType().code(),
                r.getDelinquencyType().name(),
                r.isPercentage(),
                r.getPenaltyPercentage(),
                r.getPenaltyAmount(),
                r.getFromDay(),
                r.getTillDay(),
                r.getPenaltyType(),
                r.getPromisesPerYear(),
                r.getPromisesPerLoan(),
                r.isCustom(),
                r.getCharityFundAccount(),
                r.getChannel(),
                r.getRecordState(),
                r.getVersion(),
                r.getCreated(),
                r.getUpdatedAt(),
                configs);
    }

    private EarlySettlementConfigResponse toResponse(EarlySettlementConfig c) {
        return new EarlySettlementConfigResponse(
                c.getId(),
                c.getDelinquencyId(),
                c.getInvoiceOrder(),
                c.getFromDay(),
                c.getTillDay(),
                c.isPercentage(),
                c.getDiscountPercentage(),
                c.getDiscountAmount(),
                c.isRange(),
                c.getRangeNo(),
                c.getMinInvoiceOrder(),
                c.getMaxInvoiceOrder(),
                c.getRecordState(),
                c.getVersion(),
                c.getCreated(),
                c.getUpdatedAt());
    }
}
