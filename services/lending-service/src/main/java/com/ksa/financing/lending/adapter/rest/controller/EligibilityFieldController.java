package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.domain.model.EligibilityFieldDefinition;
import com.ksa.financing.lending.domain.model.ProductEligibilityField;
import com.ksa.financing.lending.domain.port.in.ManageEligibilityFieldsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/eligibility-fields")
@RequiredArgsConstructor
@Tag(name = "Eligibility Fields", description = "Dynamic eligibility field definitions and product mappings")
public class EligibilityFieldController {

    private final ManageEligibilityFieldsUseCase useCase;

    // ══════════ PUBLIC: Get fields for product (from local DB) ══════════

    @SecuredEndpoint(obj = "eligibility-fields", act = "read")
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get eligibility fields for a product")
    public ResponseEntity<List<EligibilityFieldResponse>> getFieldsForProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var fields = useCase.getFieldsForProduct(tenantId, productId);
        var responses = fields.stream().map(EligibilityFieldResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    // ══════════ ADMIN: Field Definition CRUD ══════════

    @SecuredEndpoint(obj = "eligibility-fields", act = "create")
    @PostMapping("/definitions")
    @Operation(summary = "Create a new eligibility field definition")
    public ResponseEntity<FieldDefinitionResponse> createDefinition(
            @Valid @RequestBody CreateFieldDefinitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var definition = useCase.createDefinition(tenantId, request.fieldKey(), request.nameEn(),
                request.nameAr(), request.descriptionEn(), request.descriptionAr(),
                request.dataType(), request.inputType(), request.placeholderEn(), request.placeholderAr(),
                request.unit(), request.minValue(), request.maxValue(), request.required(), request.sortOrder());

        return ResponseEntity.status(HttpStatus.CREATED).body(FieldDefinitionResponse.from(definition));
    }

    @SecuredEndpoint(obj = "eligibility-fields", act = "update")
    @PutMapping("/definitions/{id}")
    @Operation(summary = "Update an eligibility field definition")
    public ResponseEntity<FieldDefinitionResponse> updateDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFieldDefinitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var definition = useCase.updateDefinition(tenantId, id, request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(),
                request.dataType(), request.inputType(), request.placeholderEn(), request.placeholderAr(),
                request.unit(), request.minValue(), request.maxValue(),
                request.required(), request.active(), request.sortOrder());

        return ResponseEntity.ok(FieldDefinitionResponse.from(definition));
    }

    @SecuredEndpoint(obj = "eligibility-fields", act = "read")
    @GetMapping("/definitions")
    @Operation(summary = "List all eligibility field definitions")
    public ResponseEntity<List<FieldDefinitionResponse>> listDefinitions(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var definitions = useCase.listDefinitions(tenantId);
        return ResponseEntity.ok(definitions.stream().map(FieldDefinitionResponse::from).toList());
    }

    @SecuredEndpoint(obj = "eligibility-fields", act = "delete")
    @DeleteMapping("/definitions/{id}")
    @Operation(summary = "Delete an eligibility field definition")
    public ResponseEntity<Void> deleteDefinition(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        useCase.deleteDefinition(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ══════════ ADMIN: Product-Field Assignments ══════════

    @SecuredEndpoint(obj = "eligibility-fields", act = "manage")
    @PostMapping("/product/{productId}/assign")
    @Operation(summary = "Assign an eligibility field to a product")
    public ResponseEntity<ProductFieldResponse> assignFieldToProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody AssignFieldRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var result = useCase.assignFieldToProduct(tenantId, productId, request.fieldId(),
                request.required(), request.sortOrder());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProductFieldResponse.from(result));
    }

    @SecuredEndpoint(obj = "eligibility-fields", act = "manage")
    @PutMapping("/product/{productId}/fields")
    @Operation(summary = "Replace all eligibility field assignments for a product")
    public ResponseEntity<List<EligibilityFieldResponse>> setProductFields(
            @PathVariable UUID productId,
            @Valid @RequestBody List<AssignFieldRequest> assignments,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        useCase.setProductFields(tenantId, productId, assignments.stream()
                .map(a -> new ManageEligibilityFieldsUseCase.FieldAssignment(
                        a.fieldId(), a.required(), a.sortOrder()))
                .toList());

        var fields = useCase.getFieldsForProduct(tenantId, productId);
        return ResponseEntity.ok(fields.stream().map(EligibilityFieldResponse::from).toList());
    }

    @SecuredEndpoint(obj = "eligibility-fields", act = "manage")
    @DeleteMapping("/product-fields/{id}")
    @Operation(summary = "Remove a field assignment from a product")
    public ResponseEntity<Void> removeFieldFromProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        useCase.removeFieldFromProduct(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ══════════ DTOs ══════════

    public record CreateFieldDefinitionRequest(
            @NotBlank(message = "Field key is required") String fieldKey,
            @NotBlank(message = "English name is required") String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            String dataType,
            String inputType,
            String placeholderEn,
            String placeholderAr,
            String unit,
            BigDecimal minValue,
            BigDecimal maxValue,
            boolean required,
            int sortOrder
    ) {}

    public record UpdateFieldDefinitionRequest(
            @NotBlank(message = "English name is required") String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            String dataType,
            String inputType,
            String placeholderEn,
            String placeholderAr,
            String unit,
            BigDecimal minValue,
            BigDecimal maxValue,
            boolean required,
            boolean active,
            int sortOrder
    ) {}

    public record AssignFieldRequest(
            @NotNull(message = "Field ID is required") UUID fieldId,
            boolean required,
            int sortOrder
    ) {}

    public record FieldDefinitionResponse(
            UUID id,
            String fieldKey,
            String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            String dataType,
            String inputType,
            String placeholderEn,
            String placeholderAr,
            String unit,
            BigDecimal minValue,
            BigDecimal maxValue,
            boolean required,
            boolean active,
            int sortOrder
    ) {
        public static FieldDefinitionResponse from(EligibilityFieldDefinition d) {
            return new FieldDefinitionResponse(
                    d.getId(), d.getFieldKey(), d.getNameEn(), d.getNameAr(),
                    d.getDescriptionEn(), d.getDescriptionAr(),
                    d.getDataType(), d.getInputType(),
                    d.getPlaceholderEn(), d.getPlaceholderAr(),
                    d.getUnit(), d.getMinValue(), d.getMaxValue(),
                    d.isRequired(), d.isActive(), d.getSortOrder()
            );
        }
    }

    /**
     * Response for product-specific eligibility field (enriched with definition details).
     * This is what the mobile app uses to render dynamic form fields.
     */
    public record EligibilityFieldResponse(
            UUID fieldId,
            String fieldKey,
            String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            String dataType,
            String inputType,
            String placeholderEn,
            String placeholderAr,
            String unit,
            BigDecimal minValue,
            BigDecimal maxValue,
            boolean required,
            int sortOrder
    ) {
        public static EligibilityFieldResponse from(ProductEligibilityField pf) {
            var def = pf.getFieldDefinition();
            if (def == null) {
                return new EligibilityFieldResponse(
                        pf.getFieldId(), null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        pf.isRequired(), pf.getSortOrder()
                );
            }
            return new EligibilityFieldResponse(
                    def.getId(), def.getFieldKey(), def.getNameEn(), def.getNameAr(),
                    def.getDescriptionEn(), def.getDescriptionAr(),
                    def.getDataType(), def.getInputType(),
                    def.getPlaceholderEn(), def.getPlaceholderAr(),
                    def.getUnit(), def.getMinValue(), def.getMaxValue(),
                    pf.isRequired(), pf.getSortOrder()
            );
        }
    }

    public record ProductFieldResponse(
            UUID id,
            UUID productId,
            UUID fieldId,
            boolean required,
            int sortOrder
    ) {
        public static ProductFieldResponse from(ProductEligibilityField pf) {
            return new ProductFieldResponse(pf.getId(), pf.getProductId(), pf.getFieldId(),
                    pf.isRequired(), pf.getSortOrder());
        }
    }

    // ══════════ Helpers ══════════

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
