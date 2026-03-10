package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreateMasterCategoryRequest;
import com.ksa.financing.product.adapter.rest.request.CreateSubCategoryRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateMasterCategoryRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateSubCategoryRequest;
import com.ksa.financing.product.adapter.rest.response.CategoryResponse;
import com.ksa.financing.product.adapter.rest.response.SubCategoryResponse;
import com.ksa.financing.product.application.mapper.CategoryMapper;
import com.ksa.financing.product.domain.port.in.ManageCategoryUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/product-categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Categories", description = "Master and sub-category management for product classification")
public class CategoryController {

    private final ManageCategoryUseCase manageCategoryUseCase;

    // --- Master Categories ---

    @SecuredEndpoint(obj = "product-categories", act = "read")
    @GetMapping
    @Operation(summary = "List master categories", description = "Returns all active master product categories for the tenant")
    public ResponseEntity<List<CategoryResponse>> listMasterCategories(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing master categories for tenantId: {}", tenantId);

        var categories = manageCategoryUseCase.listMasterCategories(tenantId);
        var response = categories.stream()
                .map(CategoryMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "product-categories", act = "create")
    @PostMapping
    @Operation(summary = "Create master category", description = "Creates a new master product category")
    public ResponseEntity<CategoryResponse> createMasterCategory(
            @Valid @RequestBody CreateMasterCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating master category code={} for tenantId={}", request.code(), tenantId);

        var category = manageCategoryUseCase.createMasterCategory(
                tenantId, request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.iconUrl(), request.sortOrder());

        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryMapper.toResponse(category));
    }

    @SecuredEndpoint(obj = "product-categories", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update master category", description = "Updates an existing master product category")
    public ResponseEntity<CategoryResponse> updateMasterCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMasterCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating master category id={} for tenantId={}", id, tenantId);

        var category = manageCategoryUseCase.updateMasterCategory(
                tenantId, id, request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.iconUrl(),
                request.sortOrder(), request.active());

        return ResponseEntity.ok(CategoryMapper.toResponse(category));
    }

    @SecuredEndpoint(obj = "product-categories", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete master category", description = "Deletes a master product category")
    public ResponseEntity<Void> deleteMasterCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deleting master category id={} for tenantId={}", id, tenantId);

        manageCategoryUseCase.deleteMasterCategory(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Sub-Categories ---

    @SecuredEndpoint(obj = "product-categories", act = "read")
    @GetMapping("/{id}/sub-categories")
    @Operation(summary = "List sub-categories", description = "Returns all active sub-categories for a given master category")
    public ResponseEntity<List<SubCategoryResponse>> listSubCategories(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing sub-categories for masterCategoryId: {} tenantId: {}", id, tenantId);

        var subCategories = manageCategoryUseCase.listSubCategories(tenantId, id);
        var response = subCategories.stream()
                .map(CategoryMapper::toSubCategoryResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "product-categories", act = "create")
    @PostMapping("/sub-categories")
    @Operation(summary = "Create sub-category", description = "Creates a new sub-category under a master category")
    public ResponseEntity<SubCategoryResponse> createSubCategory(
            @Valid @RequestBody CreateSubCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating sub-category code={} for master={} tenantId={}", request.code(), request.masterCategoryId(), tenantId);

        var subCategory = manageCategoryUseCase.createSubCategory(
                tenantId, request.masterCategoryId(), request.code(),
                request.nameEn(), request.nameAr(), request.sortOrder());

        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryMapper.toSubCategoryResponse(subCategory));
    }

    @SecuredEndpoint(obj = "product-categories", act = "update")
    @PutMapping("/sub-categories/{id}")
    @Operation(summary = "Update sub-category", description = "Updates an existing sub-category")
    public ResponseEntity<SubCategoryResponse> updateSubCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating sub-category id={} for tenantId={}", id, tenantId);

        var subCategory = manageCategoryUseCase.updateSubCategory(
                tenantId, id, request.nameEn(), request.nameAr(),
                request.sortOrder(), request.active());

        return ResponseEntity.ok(CategoryMapper.toSubCategoryResponse(subCategory));
    }

    @SecuredEndpoint(obj = "product-categories", act = "delete")
    @DeleteMapping("/sub-categories/{id}")
    @Operation(summary = "Delete sub-category", description = "Deletes a sub-category")
    public ResponseEntity<Void> deleteSubCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deleting sub-category id={} for tenantId={}", id, tenantId);

        manageCategoryUseCase.deleteSubCategory(tenantId, id);
        return ResponseEntity.noContent().build();
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
