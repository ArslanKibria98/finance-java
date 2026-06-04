package com.ksa.financing.customer.adapter.rest.controller;

import com.ksa.financing.customer.application.dto.CreateNetWorthRangeRequest;
import com.ksa.financing.customer.application.dto.CreateReferenceDataRequest;
import com.ksa.financing.customer.application.dto.NetWorthRangeResponse;
import com.ksa.financing.customer.application.dto.ReferenceDataResponse;
import com.ksa.financing.customer.application.dto.UpdateNetWorthRangeRequest;
import com.ksa.financing.customer.application.dto.UpdateReferenceDataRequest;
import com.ksa.financing.customer.domain.model.CanadianBankOption;
import com.ksa.financing.customer.domain.model.NetWorthRangeOption;
import com.ksa.financing.customer.domain.model.OccupationOption;
import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.customer.domain.model.RelationshipOption;
import com.ksa.financing.customer.domain.model.SourceOfFundsOption;
import com.ksa.financing.customer.domain.model.SourceOfIncomeOption;
import com.ksa.financing.customer.domain.model.SourceOfWealthOption;
import com.ksa.financing.customer.domain.port.in.GetCanadianBankUseCase;
import com.ksa.financing.customer.domain.port.in.ManageNetWorthRangeUseCase;
import com.ksa.financing.customer.domain.port.in.ManageOccupationUseCase;
import com.ksa.financing.customer.domain.port.in.ManagePurposeOfFinanceUseCase;
import com.ksa.financing.customer.domain.port.in.ManageRelationshipUseCase;
import com.ksa.financing.customer.domain.port.in.ManageSourceOfFundsUseCase;
import com.ksa.financing.customer.domain.port.in.ManageSourceOfIncomeUseCase;
import com.ksa.financing.customer.domain.port.in.ManageSourceOfWealthUseCase;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reference-data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EDD Reference Data", description = "Admin-managed reference data (Source of Wealth, Funds, Income, Occupation, Purpose of Finance, Net Worth)")
public class ReferenceDataController {

    private final ManageSourceOfWealthUseCase sourceOfWealthUseCase;
    private final ManageSourceOfFundsUseCase sourceOfFundsUseCase;
    private final ManageSourceOfIncomeUseCase sourceOfIncomeUseCase;
    private final ManageOccupationUseCase occupationUseCase;
    private final ManagePurposeOfFinanceUseCase purposeOfFinanceUseCase;
    private final ManageNetWorthRangeUseCase netWorthRangeUseCase;
    private final ManageRelationshipUseCase relationshipUseCase;
    private final GetCanadianBankUseCase canadianBankUseCase;

    // ========================================================================
    // SOURCE OF WEALTH
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "create")
    @PostMapping("/source-of-wealth")
    @Operation(summary = "Create source of wealth option", description = "Creates a new admin-managed source of wealth dropdown option")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<ReferenceDataResponse> createSourceOfWealth(
            @Valid @RequestBody CreateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating source of wealth option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManageSourceOfWealthUseCase.CreateSourceOfWealthCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.displayOrder());

        SourceOfWealthOption created = sourceOfWealthUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "read")
    @GetMapping("/source-of-wealth")
    @Operation(summary = "List all source of wealth options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllSourceOfWealth(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = sourceOfWealthUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/source-of-wealth/active")
    @Operation(summary = "List active source of wealth options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActiveSourceOfWealth(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = sourceOfWealthUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "read")
    @GetMapping("/source-of-wealth/{id}")
    @Operation(summary = "Get source of wealth option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getSourceOfWealthById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        SourceOfWealthOption option = sourceOfWealthUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "update")
    @PutMapping("/source-of-wealth/{id}")
    @Operation(summary = "Update source of wealth option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> updateSourceOfWealth(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating source of wealth option: {} for tenant: {}", id, tenantId);

        var command = new ManageSourceOfWealthUseCase.UpdateSourceOfWealthCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.isActive(), request.displayOrder());

        SourceOfWealthOption updated = sourceOfWealthUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "delete")
    @DeleteMapping("/source-of-wealth/{id}")
    @Operation(summary = "Permanently delete source of wealth option", description = "Soft-deletes the option permanently (is_deleted = true). Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deleteSourceOfWealth(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Soft-deleting source of wealth option: {} for tenant: {}", id, tenantId);
        sourceOfWealthUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "update")
    @PostMapping("/source-of-wealth/{id}/activate")
    @Operation(summary = "Activate source of wealth option", description = "Sets is_active = true")
    @ApiResponse(responseCode = "204", description = "Option activated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> activateSourceOfWealth(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfWealthUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.source-of-wealth", act = "update")
    @PostMapping("/source-of-wealth/{id}/deactivate")
    @Operation(summary = "Deactivate source of wealth option", description = "Sets is_active = false")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deactivateSourceOfWealth(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfWealthUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // SOURCE OF FUNDS
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "create")
    @PostMapping("/source-of-funds")
    @Operation(summary = "Create source of funds option", description = "Creates a new admin-managed source of funds dropdown option")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<ReferenceDataResponse> createSourceOfFunds(
            @Valid @RequestBody CreateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating source of funds option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManageSourceOfFundsUseCase.CreateSourceOfFundsCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.displayOrder());

        SourceOfFundsOption created = sourceOfFundsUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "read")
    @GetMapping("/source-of-funds")
    @Operation(summary = "List all source of funds options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllSourceOfFunds(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = sourceOfFundsUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/source-of-funds/active")
    @Operation(summary = "List active source of funds options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActiveSourceOfFunds(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = sourceOfFundsUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "read")
    @GetMapping("/source-of-funds/{id}")
    @Operation(summary = "Get source of funds option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getSourceOfFundsById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        SourceOfFundsOption option = sourceOfFundsUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "update")
    @PutMapping("/source-of-funds/{id}")
    @Operation(summary = "Update source of funds option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> updateSourceOfFunds(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating source of funds option: {} for tenant: {}", id, tenantId);

        var command = new ManageSourceOfFundsUseCase.UpdateSourceOfFundsCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.isActive(), request.displayOrder());

        SourceOfFundsOption updated = sourceOfFundsUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "delete")
    @DeleteMapping("/source-of-funds/{id}")
    @Operation(summary = "Permanently delete source of funds option", description = "Soft-deletes the option permanently. Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deleteSourceOfFunds(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfFundsUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "update")
    @PostMapping("/source-of-funds/{id}/activate")
    @Operation(summary = "Activate source of funds option")
    @ApiResponse(responseCode = "204", description = "Option activated")
    public ResponseEntity<Void> activateSourceOfFunds(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfFundsUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.source-of-funds", act = "update")
    @PostMapping("/source-of-funds/{id}/deactivate")
    @Operation(summary = "Deactivate source of funds option")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    public ResponseEntity<Void> deactivateSourceOfFunds(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfFundsUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // SOURCE OF INCOME
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "create")
    @PostMapping("/source-of-income")
    @Operation(summary = "Create source of income option", description = "Creates a new admin-managed source of income dropdown option")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<ReferenceDataResponse> createSourceOfIncome(
            @Valid @RequestBody CreateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating source of income option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManageSourceOfIncomeUseCase.CreateSourceOfIncomeCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.displayOrder());

        SourceOfIncomeOption created = sourceOfIncomeUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "read")
    @GetMapping("/source-of-income")
    @Operation(summary = "List all source of income options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllSourceOfIncome(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = sourceOfIncomeUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/source-of-income/active")
    @Operation(summary = "List active source of income options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActiveSourceOfIncome(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = sourceOfIncomeUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "read")
    @GetMapping("/source-of-income/{id}")
    @Operation(summary = "Get source of income option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getSourceOfIncomeById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        SourceOfIncomeOption option = sourceOfIncomeUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "update")
    @PutMapping("/source-of-income/{id}")
    @Operation(summary = "Update source of income option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> updateSourceOfIncome(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating source of income option: {} for tenant: {}", id, tenantId);

        var command = new ManageSourceOfIncomeUseCase.UpdateSourceOfIncomeCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.isActive(), request.displayOrder());

        SourceOfIncomeOption updated = sourceOfIncomeUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "delete")
    @DeleteMapping("/source-of-income/{id}")
    @Operation(summary = "Permanently delete source of income option", description = "Soft-deletes the option permanently. Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deleteSourceOfIncome(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfIncomeUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "update")
    @PostMapping("/source-of-income/{id}/activate")
    @Operation(summary = "Activate source of income option")
    @ApiResponse(responseCode = "204", description = "Option activated")
    public ResponseEntity<Void> activateSourceOfIncome(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfIncomeUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.source-of-income", act = "update")
    @PostMapping("/source-of-income/{id}/deactivate")
    @Operation(summary = "Deactivate source of income option")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    public ResponseEntity<Void> deactivateSourceOfIncome(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        sourceOfIncomeUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // OCCUPATION
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.occupation", act = "create")
    @PostMapping("/occupation")
    @Operation(summary = "Create occupation option", description = "Creates a new admin-managed occupation dropdown option")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<ReferenceDataResponse> createOccupation(
            @Valid @RequestBody CreateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating occupation option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManageOccupationUseCase.CreateOccupationCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.displayOrder());

        OccupationOption created = occupationUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.occupation", act = "read")
    @GetMapping("/occupation")
    @Operation(summary = "List all occupation options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllOccupation(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = occupationUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/occupation/active")
    @Operation(summary = "List active occupation options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActiveOccupation(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = occupationUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.occupation", act = "read")
    @GetMapping("/occupation/{id}")
    @Operation(summary = "Get occupation option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getOccupationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        OccupationOption option = occupationUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.occupation", act = "update")
    @PutMapping("/occupation/{id}")
    @Operation(summary = "Update occupation option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> updateOccupation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating occupation option: {} for tenant: {}", id, tenantId);

        var command = new ManageOccupationUseCase.UpdateOccupationCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.isActive(), request.displayOrder());

        OccupationOption updated = occupationUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.occupation", act = "delete")
    @DeleteMapping("/occupation/{id}")
    @Operation(summary = "Permanently delete occupation option", description = "Soft-deletes the option permanently. Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deleteOccupation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        occupationUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.occupation", act = "update")
    @PostMapping("/occupation/{id}/activate")
    @Operation(summary = "Activate occupation option")
    @ApiResponse(responseCode = "204", description = "Option activated")
    public ResponseEntity<Void> activateOccupation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        occupationUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.occupation", act = "update")
    @PostMapping("/occupation/{id}/deactivate")
    @Operation(summary = "Deactivate occupation option")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    public ResponseEntity<Void> deactivateOccupation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        occupationUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // PURPOSE OF FINANCE
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "create")
    @PostMapping("/purpose-of-finance")
    @Operation(summary = "Create purpose of finance option", description = "Creates a new admin-managed purpose of finance dropdown option")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<ReferenceDataResponse> createPurposeOfFinance(
            @Valid @RequestBody CreateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating purpose of finance option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManagePurposeOfFinanceUseCase.CreatePurposeOfFinanceCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.displayOrder());

        PurposeOfFinanceOption created = purposeOfFinanceUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "read")
    @GetMapping("/purpose-of-finance")
    @Operation(summary = "List all purpose of finance options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllPurposeOfFinance(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = purposeOfFinanceUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/purpose-of-finance/active")
    @Operation(summary = "List active purpose of finance options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActivePurposeOfFinance(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = purposeOfFinanceUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "read")
    @GetMapping("/purpose-of-finance/{id}")
    @Operation(summary = "Get purpose of finance option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getPurposeOfFinanceById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        PurposeOfFinanceOption option = purposeOfFinanceUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "update")
    @PutMapping("/purpose-of-finance/{id}")
    @Operation(summary = "Update purpose of finance option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> updatePurposeOfFinance(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating purpose of finance option: {} for tenant: {}", id, tenantId);

        var command = new ManagePurposeOfFinanceUseCase.UpdatePurposeOfFinanceCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.isActive(), request.displayOrder());

        PurposeOfFinanceOption updated = purposeOfFinanceUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "delete")
    @DeleteMapping("/purpose-of-finance/{id}")
    @Operation(summary = "Permanently delete purpose of finance option", description = "Soft-deletes the option permanently. Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deletePurposeOfFinance(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        purposeOfFinanceUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "update")
    @PostMapping("/purpose-of-finance/{id}/activate")
    @Operation(summary = "Activate purpose of finance option")
    @ApiResponse(responseCode = "204", description = "Option activated")
    public ResponseEntity<Void> activatePurposeOfFinance(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        purposeOfFinanceUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.purpose-of-finance", act = "update")
    @PostMapping("/purpose-of-finance/{id}/deactivate")
    @Operation(summary = "Deactivate purpose of finance option")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    public ResponseEntity<Void> deactivatePurposeOfFinance(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        purposeOfFinanceUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // NET WORTH RANGES
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "create")
    @PostMapping("/net-worth-ranges")
    @Operation(summary = "Create net worth range option", description = "Creates a new admin-managed net worth range dropdown option")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<NetWorthRangeResponse> createNetWorthRange(
            @Valid @RequestBody CreateNetWorthRangeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating net worth range option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManageNetWorthRangeUseCase.CreateNetWorthRangeCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.minValue(), request.maxValue(), request.displayOrder());

        NetWorthRangeOption created = netWorthRangeUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toNetWorthResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "read")
    @GetMapping("/net-worth-ranges")
    @Operation(summary = "List all net worth range options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<NetWorthRangeResponse>> getAllNetWorthRanges(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = netWorthRangeUseCase.getAll(tenantId, pageQuery).map(this::toNetWorthResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/net-worth-ranges/active")
    @Operation(summary = "List active net worth range options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<NetWorthRangeResponse>> getActiveNetWorthRanges(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = netWorthRangeUseCase.getActive(tenantId, pageQuery).map(this::toNetWorthResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "read")
    @GetMapping("/net-worth-ranges/{id}")
    @Operation(summary = "Get net worth range option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<NetWorthRangeResponse> getNetWorthRangeById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        NetWorthRangeOption option = netWorthRangeUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toNetWorthResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "update")
    @PutMapping("/net-worth-ranges/{id}")
    @Operation(summary = "Update net worth range option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<NetWorthRangeResponse> updateNetWorthRange(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNetWorthRangeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating net worth range option: {} for tenant: {}", id, tenantId);

        var command = new ManageNetWorthRangeUseCase.UpdateNetWorthRangeCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.minValue(), request.maxValue(),
                request.isActive(), request.displayOrder());

        NetWorthRangeOption updated = netWorthRangeUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toNetWorthResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "delete")
    @DeleteMapping("/net-worth-ranges/{id}")
    @Operation(summary = "Permanently delete net worth range option", description = "Soft-deletes the option permanently. Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deleteNetWorthRange(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        netWorthRangeUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "update")
    @PostMapping("/net-worth-ranges/{id}/activate")
    @Operation(summary = "Activate net worth range option")
    @ApiResponse(responseCode = "204", description = "Option activated")
    public ResponseEntity<Void> activateNetWorthRange(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        netWorthRangeUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.net-worth-ranges", act = "update")
    @PostMapping("/net-worth-ranges/{id}/deactivate")
    @Operation(summary = "Deactivate net worth range option")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    public ResponseEntity<Void> deactivateNetWorthRange(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        netWorthRangeUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // RELATIONSHIP
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.relationship", act = "create")
    @PostMapping("/relationship")
    @Operation(summary = "Create relationship option", description = "Creates a new admin-managed relationship dropdown option (e.g., Father, Mother, Spouse)")
    @ApiResponse(responseCode = "201", description = "Option created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code")
    public ResponseEntity<ReferenceDataResponse> createRelationship(
            @Valid @RequestBody CreateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating relationship option: {} for tenant: {}", request.code(), tenantId);

        var command = new ManageRelationshipUseCase.CreateRelationshipCommand(
                request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.displayOrder());

        RelationshipOption created = relationshipUseCase.create(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @SecuredEndpoint(obj = "reference-data.relationship", act = "read")
    @GetMapping("/relationship")
    @Operation(summary = "List all relationship options", description = "Returns all options including inactive (admin view)")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllRelationship(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = relationshipUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/relationship/active")
    @Operation(summary = "List active relationship options",
               description = "Returns only active options sorted by display order (for dropdown population). "
                           + "Public endpoint — accepts X-Tenant-Id header or JWT for tenant identification.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActiveRelationship(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantIdFromJwtOrHeader(jwt, httpRequest);
        var responses = relationshipUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.relationship", act = "read")
    @GetMapping("/relationship/{id}")
    @Operation(summary = "Get relationship option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getRelationshipById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        RelationshipOption option = relationshipUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    @SecuredEndpoint(obj = "reference-data.relationship", act = "update")
    @PutMapping("/relationship/{id}")
    @Operation(summary = "Update relationship option", description = "Updates an existing option (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Option updated")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> updateRelationship(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating relationship option: {} for tenant: {}", id, tenantId);

        var command = new ManageRelationshipUseCase.UpdateRelationshipCommand(
                request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.isActive(), request.displayOrder());

        RelationshipOption updated = relationshipUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(updated));
    }

    @SecuredEndpoint(obj = "reference-data.relationship", act = "delete")
    @DeleteMapping("/relationship/{id}")
    @Operation(summary = "Permanently delete relationship option", description = "Soft-deletes the option permanently. Use activate/deactivate for toggling visibility.")
    @ApiResponse(responseCode = "204", description = "Option deleted")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<Void> deleteRelationship(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        relationshipUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.relationship", act = "update")
    @PostMapping("/relationship/{id}/activate")
    @Operation(summary = "Activate relationship option")
    @ApiResponse(responseCode = "204", description = "Option activated")
    public ResponseEntity<Void> activateRelationship(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        relationshipUseCase.activate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "reference-data.relationship", act = "update")
    @PostMapping("/relationship/{id}/deactivate")
    @Operation(summary = "Deactivate relationship option")
    @ApiResponse(responseCode = "204", description = "Option deactivated")
    public ResponseEntity<Void> deactivateRelationship(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        relationshipUseCase.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // CANADIAN BANKS (read-only LOV — token required, not public)
    // ========================================================================

    @SecuredEndpoint(obj = "reference-data.canadian-bank", act = "read")
    @GetMapping("/canadian-bank")
    @Operation(summary = "List all Canadian bank options",
               description = "Returns all Canadian financial institutions including inactive (admin view). Requires JWT.")
    @ApiResponse(responseCode = "200", description = "Options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getAllCanadianBank(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = canadianBankUseCase.getAll(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.canadian-bank", act = "read")
    @GetMapping("/canadian-bank/active")
    @Operation(summary = "List active Canadian bank options",
               description = "Returns only active Canadian financial institutions sorted by display order (for dropdown population). Requires JWT.")
    @ApiResponse(responseCode = "200", description = "Active options retrieved")
    public ResponseEntity<PageResponse<ReferenceDataResponse>> getActiveCanadianBank(
            @AuthenticationPrincipal Jwt jwt,
            PageQuery pageQuery) {

        UUID tenantId = extractTenantId(jwt);
        var responses = canadianBankUseCase.getActive(tenantId, pageQuery).map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "reference-data.canadian-bank", act = "read")
    @GetMapping("/canadian-bank/{id}")
    @Operation(summary = "Get Canadian bank option by ID")
    @ApiResponse(responseCode = "200", description = "Option found")
    @ApiResponse(responseCode = "404", description = "Option not found")
    public ResponseEntity<ReferenceDataResponse> getCanadianBankById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        CanadianBankOption option = canadianBankUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(option));
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    /**
     * Extracts tenant ID from JWT (authenticated endpoints) or X-Tenant-Id header (public endpoints).
     * Public /active endpoints may be called by other services without JWT.
     */
    private UUID extractTenantIdFromJwtOrHeader(Jwt jwt, HttpServletRequest httpRequest) {
        // Try JWT first
        if (jwt != null) {
            var tenantClaim = jwt.getClaimAsString("tenant_id");
            if (tenantClaim != null) {
                return UUID.fromString(tenantClaim);
            }
        }
        // Fall back to X-Tenant-Id header
        var tenantHeader = httpRequest.getHeader("X-Tenant-Id");
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                return UUID.fromString(tenantHeader);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(
                        ErrorCodes.BAD_REQUEST,
                        "Invalid X-Tenant-Id header format (must be UUID): " + tenantHeader,
                        "Invalid X-Tenant-Id header format (must be UUID): " + tenantHeader);
            }
        }
        throw new BusinessException(
                ErrorCodes.BAD_REQUEST,
                "Tenant identification required: provide JWT with tenant_id claim or X-Tenant-Id header",
                "Tenant identification required: provide JWT with tenant_id claim or X-Tenant-Id header");
    }

    private ReferenceDataResponse toResponse(SourceOfWealthOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ReferenceDataResponse toResponse(SourceOfIncomeOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ReferenceDataResponse toResponse(PurposeOfFinanceOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ReferenceDataResponse toResponse(SourceOfFundsOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ReferenceDataResponse toResponse(OccupationOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ReferenceDataResponse toResponse(RelationshipOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ReferenceDataResponse toResponse(CanadianBankOption o) {
        return new ReferenceDataResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private NetWorthRangeResponse toNetWorthResponse(NetWorthRangeOption o) {
        return new NetWorthRangeResponse(
                o.getId(), o.getCode(), o.getNameEn(), o.getNameAr(),
                o.getDescriptionEn(), o.getDescriptionAr(),
                o.getMinValue(), o.getMaxValue(),
                o.isActive(), o.getDisplayOrder(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
