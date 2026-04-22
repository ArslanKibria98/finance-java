package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.application.dto.AssignAccountToProductFieldRequest;
import com.ksa.financing.ledger.application.dto.AssignCoaAccountsRequest;
import com.ksa.financing.ledger.application.dto.CoaConfigurationMappingResponse;
import com.ksa.financing.ledger.application.dto.ProductCoaSelectionResponse;
import com.ksa.financing.ledger.application.dto.ProductCoaFieldResponse;
import com.ksa.financing.ledger.application.dto.SelectCoaFieldsRequest;
import com.ksa.financing.ledger.application.mapper.CoaConfigurationMapper;
import com.ksa.financing.ledger.domain.port.in.ManageCoaConfigurationUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import com.ksa.financing.ledger.infrastructure.persistence.entity.ProductCoaAccountMappingJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaProductCoaAccountMappingRepository;
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

/**
 * Product ↔ COA Field ↔ Account Mapping Controller
 * Maps COA fields to products and links selected accounts
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product COA Fields", description = "Map COA fields to products and assign accounts")
public class ProductCoaFieldController {

    private final JpaProductCoaAccountMappingRepository mappingRepository;
    private final AccountRepository accountRepository;
    private final CoaFieldLovRepository coaFieldLovRepository;
    private final ManageCoaConfigurationUseCase manageCoaConfigurationUseCase;
    private final CoaConfigurationMapper coaConfigurationMapper;

    /**
     * Get all COA fields assigned to a product
     * GET /api/v1/products/{productId}/coa-fields
     */
    @SecuredEndpoint(obj = "products.coa-fields", act = "read")
    @GetMapping("/{productId}/coa-fields")
    @Operation(summary = "Get COA fields assigned to product",
               description = "Returns all COA fields with their assigned accounts for the given product")
    public ResponseEntity<List<ProductCoaSelectionResponse>> getProductCoaFields(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Getting COA mappings for product: productId={} tenant={}", productId, tenantId);
        var profile = manageCoaConfigurationUseCase.getOrCreateProfileByProduct(tenantId, productId);
        var mappings = manageCoaConfigurationUseCase.getMappings(tenantId, profile.getId());
        var responses = mappings.stream()
                .map(m -> coaConfigurationMapper.toResponse(m, productId))
                .map(r -> toProductResponse(tenantId, r))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "update")
    @PutMapping("/{productId}/coa-fields")
    @Operation(summary = "Select multiple COA fields by product id")
    public ResponseEntity<List<ProductCoaSelectionResponse>> selectFieldsForProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody SelectCoaFieldsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var profile = manageCoaConfigurationUseCase.getOrCreateProfileByProduct(tenantId, productId);
        var mappings = manageCoaConfigurationUseCase.selectFields(new ManageCoaConfigurationUseCase.SelectFieldsCommand(
                tenantId,
                profile.getId(),
                request.fieldKeys()
        ));
        return ResponseEntity.ok(mappings.stream()
                .map(m -> coaConfigurationMapper.toResponse(m, productId))
                .map(r -> toProductResponse(tenantId, r))
                .toList());
    }

    @SecuredEndpoint(obj = "ledger.coa-config", act = "update")
    @PutMapping("/{productId}/coa-fields/accounts")
    @Operation(summary = "Assign multiple accounts by product id")
    public ResponseEntity<List<ProductCoaSelectionResponse>> assignAccountsForProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody AssignCoaAccountsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var profile = manageCoaConfigurationUseCase.getOrCreateProfileByProduct(tenantId, productId);
        var assignments = request.assignments().stream()
                .map(a -> new ManageCoaConfigurationUseCase.AssignmentItem(a.fieldKey(), a.accountCode()))
                .toList();
        var mappings = manageCoaConfigurationUseCase.assignAccounts(new ManageCoaConfigurationUseCase.AssignAccountsCommand(
                tenantId,
                profile.getId(),
                assignments
        ));
        return ResponseEntity.ok(mappings.stream()
                .map(m -> coaConfigurationMapper.toResponse(m, productId))
                .map(r -> toProductResponse(tenantId, r))
                .toList());
    }

    /**
     * Assign account to a product's COA field
     * POST /api/v1/products/{productId}/coa-fields/{fieldId}/accounts
     *
     * Flow:
     * 1. User selects a product
     * 2. System shows available COA fields for that product
     * 3. User selects a COA field
     * 4. User selects an account to map against that field
     * 5. This endpoint stores: ProductId + CoaFieldId + SelectedAccountId
     */
    @SecuredEndpoint(obj = "products.coa-fields", act = "create")
    @PostMapping("/{productId}/coa-fields/{fieldId}/accounts")
    @Operation(summary = "Assign account to product COA field",
               description = "Links a GL account to a COA field for a specific product. " +
                           "Stores: ProductId + CoaFieldId + AccountId mapping")
    public ResponseEntity<ProductCoaFieldResponse> assignAccountToProductField(
            @PathVariable UUID productId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody AssignAccountToProductFieldRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Assigning account to product field: productId={} fieldId={} accountId={} tenant={}",
                productId, fieldId, request.accountId(), tenantId);

        // Verify account exists
        var accountId = com.ksa.financing.ledger.domain.model.AccountId.of(request.accountId());
        var account = accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> NotFoundException.forEntity("Account", request.accountId().toString()));

        // Check if mapping already exists
        var existing = mappingRepository.findByTenantIdAndProductIdAndCoaFieldId(tenantId, productId, fieldId);
        if (existing.isPresent()) {
            throw new BusinessException(
                    ErrorCodes.CONFLICT,
                    "Account already assigned to this product field");
        }

        // Create mapping
        var mapping = ProductCoaAccountMappingJpaEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(productId)
                .coaFieldId(fieldId)
                .accountId(request.accountId())
                .accountCode(request.accountCode())
                .status("ACTIVE")
                .notes(request.notes())
                .build();

        var saved = mappingRepository.save(mapping);
        log.info("Account assigned to product field: mappingId={}", saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ProductCoaFieldResponse(
                        saved.getId(),
                        saved.getProductId(),
                        saved.getCoaFieldId(),
                        "FIELD_CODE", // TODO: Join
                        "Field Name", // TODO: Join
                        "نام", // TODO: Join
                        "ASSET", // TODO: Join
                        true, // TODO: Join
                        saved.getAccountId(),
                        saved.getAccountCode(),
                        account.getAccountName(),
                        saved.getStatus(),
                        saved.getCreatedAt()
                )
        );
    }

    /**
     * Get assigned account for a product's COA field
     * GET /api/v1/products/{productId}/coa-fields/{fieldId}/account
     */
    @SecuredEndpoint(obj = "products.coa-fields", act = "read")
    @GetMapping("/{productId}/coa-fields/{fieldId}/account")
    @Operation(summary = "Get assigned account for product field",
               description = "Returns the account assigned to a specific COA field for a product")
    public ResponseEntity<ProductCoaFieldResponse> getAssignedAccount(
            @PathVariable UUID productId,
            @PathVariable UUID fieldId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var mapping = mappingRepository.findByTenantIdAndProductIdAndCoaFieldId(tenantId, productId, fieldId)
                .orElseThrow(() -> NotFoundException.forEntity("ProductCoaFieldMapping",
                        productId + "/" + fieldId));

        var accountId = com.ksa.financing.ledger.domain.model.AccountId.of(mapping.getAccountId());
        var account = accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> NotFoundException.forEntity("Account", mapping.getAccountId().toString()));

        return ResponseEntity.ok(
                new ProductCoaFieldResponse(
                        mapping.getId(),
                        mapping.getProductId(),
                        mapping.getCoaFieldId(),
                        "FIELD_CODE", // TODO: Join
                        "Field Name", // TODO: Join
                        "نام", // TODO: Join
                        "ASSET", // TODO: Join
                        true, // TODO: Join
                        mapping.getAccountId(),
                        mapping.getAccountCode(),
                        account.getAccountName(),
                        mapping.getStatus(),
                        mapping.getUpdatedAt()
                )
        );
    }

    /**
     * Unassign account from product field
     * DELETE /api/v1/products/{productId}/coa-fields/{fieldId}/account
     */
    @SecuredEndpoint(obj = "products.coa-fields", act = "delete")
    @DeleteMapping("/{productId}/coa-fields/{fieldId}/account")
    @Operation(summary = "Unassign account from product field",
               description = "Removes the account assignment from a product's COA field")
    public ResponseEntity<Void> unassignAccount(
            @PathVariable UUID productId,
            @PathVariable UUID fieldId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Unassigning account from product field: productId={} fieldId={} tenant={}",
                productId, fieldId, tenantId);

        mappingRepository.deleteByTenantIdAndProductIdAndCoaFieldId(tenantId, productId, fieldId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private ProductCoaSelectionResponse toProductResponse(UUID tenantId, CoaConfigurationMappingResponse response) {
        var fieldDetails = resolveFieldDetails(tenantId, response.coaFieldId());
        var accountDetails = resolveAccountDetails(tenantId, response.accountId());
        return new ProductCoaSelectionResponse(
                response.id(),
                response.tenantId(),
                response.productId(),
                response.coaFieldId(),
                fieldDetails.key(),
                fieldDetails.label(),
                response.accountId(),
                accountDetails.code(),
                accountDetails.name(),
                response.mandatoryOverride(),
                response.notes(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private AccountDetails resolveAccountDetails(UUID tenantId, UUID accountId) {
        if (accountId == null) {
            return new AccountDetails(null, null);
        }
        var account = accountRepository.findById(tenantId, com.ksa.financing.ledger.domain.model.AccountId.of(accountId));
        if (account.isEmpty()) {
            return new AccountDetails(null, null);
        }
        return new AccountDetails(account.get().getAccountCode(), account.get().getAccountName());
    }

    private FieldDetails resolveFieldDetails(UUID tenantId, UUID fieldId) {
        var field = coaFieldLovRepository.findById(tenantId, fieldId);
        if (field.isEmpty()) {
            return new FieldDetails(null, null);
        }
        return new FieldDetails(field.get().getFieldKey(), field.get().getFieldLabelEn());
    }

    private record AccountDetails(String code, String name) {}
    private record FieldDetails(String key, String label) {}
}
