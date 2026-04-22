package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.*;
import com.ksa.financing.product.adapter.rest.response.ProductResponse.DurationSettingsResponse;
import com.ksa.financing.product.domain.port.in.ManageProductSettingsUseCase;
import com.ksa.financing.product.domain.port.in.ManageProductSettingsUseCase.*;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Settings", description = "Settings tabs for product wizard step 3")
public class ProductSettingsController {

    private final ManageProductSettingsUseCase manageProductSettingsUseCase;

    // --- Tab 1: Application Steps ---

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/application-steps")
    @Operation(summary = "Update application steps", description = "Replaces all application workflow steps for this product")
    public ResponseEntity<Void> updateApplicationSteps(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateApplicationStepsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating application steps for product: {} tenant: {}", productId, tenantId);

        var commands = request.steps().stream()
            .map(s -> new ApplicationStepCommand(s.stepNumber(), s.titleEn(), s.titleAr(),
                s.description(), s.required(), s.sortOrder()))
            .toList();

        manageProductSettingsUseCase.updateApplicationSteps(tenantId, productId, commands);
        return ResponseEntity.ok().build();
    }

    // --- Tab 2: Terms & Conditions ---

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/terms-conditions")
    @Operation(summary = "Update terms and conditions", description = "Updates bilingual terms and conditions")
    public ResponseEntity<Void> updateTermsConditions(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateTermsConditionsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating terms and conditions for product: {} tenant: {}", productId, tenantId);

        var command = new UpdateTermsConditionsCommand(request.termsEn(), request.termsAr());
        manageProductSettingsUseCase.updateTermsConditions(tenantId, productId, command);
        return ResponseEntity.ok().build();
    }

    // --- Tab 3: Fee Settings ---

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/fee-settings")
    @Operation(summary = "Update fee settings", description = "Updates product fee and DBR configuration")
    public ResponseEntity<Void> updateFeeSettings(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateFeeSettingsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating fee settings for product: {} tenant: {}", productId, tenantId);

        var command = new UpdateFeeSettingsCommand(
                request.revenueEligibilityThreshold(),
                request.maxDbrPercentage(),
                request.dbrCalculationMethod(), request.dbrExceptions(),
                request.maxDti(), request.minAge(), request.maxAge(),
                request.gdbrPercentage()
        );

        manageProductSettingsUseCase.updateFeeSettings(tenantId, productId, command);
        return ResponseEntity.ok().build();
    }

    // --- Tab 4: Admin Fee Slabs ---

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/admin-fee-slabs")
    @Operation(summary = "Update admin fee slabs", description = "Replaces all tiered admin fee slabs for this product")
    public ResponseEntity<Void> updateAdminFeeSlabs(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateAdminFeeSlabsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating admin fee slabs for product: {} tenant: {}", productId, tenantId);

        var commands = request.slabs().stream()
            .map(s -> new AdminFeeSlabCommand(s.minAmount(), s.maxAmount(),
                s.profitPercentage(), s.processingFee(), s.adminFee(),
                s.partnerScope(), s.status(), s.sortOrder(),
                s.minTenure(), s.maxTenure()))
            .toList();

        manageProductSettingsUseCase.updateAdminFeeSlabs(tenantId, productId, commands);
        return ResponseEntity.ok().build();
    }

    // --- Tab 5: Environment Configs ---

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/environment-configs")
    @Operation(summary = "Update environment configs", description = "Links/unlinks third-party integrations to this product")
    public ResponseEntity<Void> updateEnvironmentConfigs(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateEnvironmentConfigsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating environment configs for product: {} tenant: {}", productId, tenantId);

        var commands = request.configs() == null ? List.<EnvironmentConfigLinkCommand>of() :
            request.configs().stream()
                .map(c -> new EnvironmentConfigLinkCommand(c.environmentConfigId(), c.active(), c.sortOrder()))
                .toList();

        manageProductSettingsUseCase.updateEnvironmentConfigs(tenantId, productId, commands);
        return ResponseEntity.ok().build();
    }

    // --- Tab 6: Duration Settings ---

    @SecuredEndpoint(obj = "product-settings", act = "read")
    @GetMapping("/duration-settings")
    @Operation(summary = "Get duration settings",
               description = "Returns current duration settings. Returns defaults (all zeros) if never saved — never 404.")
    public ResponseEntity<DurationSettingsResponse> getDurationSettings(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Fetching duration settings for product: {} tenant: {}", productId, tenantId);

        var ds = manageProductSettingsUseCase.getDurationSettings(tenantId, productId);
        return ResponseEntity.ok(new DurationSettingsResponse(
                ds.id(),
                ds.requestDurationDays(),
                ds.approvalDurationDays(),
                ds.disbursementDurationHours(),
                ds.repaymentDurationDays()));
    }

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/duration-settings")
    @Operation(summary = "Update duration settings",
               description = "Upserts duration settings for this product. All fields allow 0 (immediate / no wait).")
    public ResponseEntity<Void> updateDurationSettings(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateDurationSettingsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating duration settings for product: {} tenant: {}", productId, tenantId);

        var command = new UpdateDurationSettingsCommand(
                request.requestDurationDaysOrZero(),
                request.approvalDurationDaysOrZero(),
                request.disbursementDurationHoursOrZero(),
                request.repaymentDurationDaysOrZero()
        );

        manageProductSettingsUseCase.updateDurationSettings(tenantId, productId, command);
        return ResponseEntity.ok().build();
    }

    // --- Tab 7: Approval Workflows ---

    @SecuredEndpoint(obj = "product-settings", act = "update")
    @PutMapping("/approval-workflows")
    @Operation(summary = "Update approval workflows", description = "Replaces all approval/rejection workflows with conditions and actions")
    public ResponseEntity<Void> updateApprovalWorkflows(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateApprovalWorkflowsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating approval workflows for product: {} tenant: {}", productId, tenantId);

        var commands = request.workflows() == null ? List.<ApprovalWorkflowCommand>of() :
            request.workflows().stream()
                .map(w -> new ApprovalWorkflowCommand(
                    w.workflowType(), w.nameEn(), w.nameAr(), w.description(),
                    w.templateSource(), w.active(), w.priority(),
                    w.conditions() == null ? List.of() : w.conditions().stream()
                        .map(c -> new ApprovalConditionCommand(c.field(), c.operator(), c.value(), c.sortOrder()))
                        .toList(),
                    w.actions() == null ? List.of() : w.actions().stream()
                        .map(a -> new ApprovalActionCommand(a.actionType(), a.configuration(), a.sortOrder()))
                        .toList()
                ))
                .toList();

        manageProductSettingsUseCase.updateApprovalWorkflows(tenantId, productId, commands);
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
