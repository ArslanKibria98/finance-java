package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.application.mapper.CategoryMapper;
import com.ksa.financing.product.adapter.rest.response.CategoryResponse;
import com.ksa.financing.product.adapter.rest.response.SubCategoryResponse;
import com.ksa.financing.product.domain.port.in.ManageCategoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/product-categories")
@RequiredArgsConstructor
@Tag(name = "Product Categories", description = "Endpoints for managing product master and sub-categories")
public class CategoryController {

    private final ManageCategoryUseCase manageCategoryUseCase;

    // --- Master Categories ---

    @SecuredEndpoint(obj = "product-categories", act = "read")
    @GetMapping
    @Operation(summary = "List all master categories", description = "Returns all master categories for the tenant")
    public PageResponse<CategoryResponse> listMasterCategories(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var page = manageCategoryUseCase.listMasterCategories(tenantId, query);
        return page.map(CategoryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "product-categories", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get master category", description = "Returns a single master category by ID")
    public ResponseEntity<CategoryResponse> getMasterCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var category = manageCategoryUseCase.getMasterCategory(tenantId, id);
        return ResponseEntity.ok(CategoryMapper.toResponse(category));
    }

    @SecuredEndpoint(obj = "product-categories", act = "manage")
    @PostMapping
    @Operation(summary = "Create master category")
    public ResponseEntity<CategoryResponse> createMasterCategory(
            @RequestParam String code,
            @RequestParam String nameEn,
            @RequestParam String nameAr,
            @RequestParam(required = false) String descriptionEn,
            @RequestParam(required = false) String descriptionAr,
            @RequestParam(required = false) String iconUrl,
            @RequestParam(defaultValue = "0") int sortOrder,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var category = manageCategoryUseCase.createMasterCategory(tenantId, code, nameEn, nameAr,
                descriptionEn, descriptionAr, iconUrl, sortOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryMapper.toResponse(category));
    }

    @SecuredEndpoint(obj = "product-categories", act = "manage")
    @PutMapping("/{id}")
    @Operation(summary = "Update master category")
    public ResponseEntity<CategoryResponse> updateMasterCategory(
            @PathVariable UUID id,
            @RequestParam String nameEn,
            @RequestParam String nameAr,
            @RequestParam(required = false) String descriptionEn,
            @RequestParam(required = false) String descriptionAr,
            @RequestParam(required = false) String iconUrl,
            @RequestParam(defaultValue = "0") int sortOrder,
            @RequestParam(defaultValue = "true") boolean active,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var category = manageCategoryUseCase.updateMasterCategory(tenantId, id, nameEn, nameAr,
                descriptionEn, descriptionAr, iconUrl, sortOrder, active);
        return ResponseEntity.ok(CategoryMapper.toResponse(category));
    }

    @SecuredEndpoint(obj = "product-categories", act = "manage")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete master category")
    public ResponseEntity<Void> deleteMasterCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        manageCategoryUseCase.deleteMasterCategory(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Sub-Categories ---

    @SecuredEndpoint(obj = "product-categories", act = "read")
    @GetMapping("/{id}/sub-categories")
    @Operation(summary = "List sub-categories", description = "Returns all sub-categories for a master category")
    public PageResponse<SubCategoryResponse> listSubCategories(
            @PathVariable UUID id,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var page = manageCategoryUseCase.listSubCategories(tenantId, id, query);
        return page.map(CategoryMapper::toSubCategoryResponse);
    }

    @SecuredEndpoint(obj = "product-categories", act = "manage")
    @PostMapping("/{id}/sub-categories")
    @Operation(summary = "Create sub-category")
    public ResponseEntity<SubCategoryResponse> createSubCategory(
            @PathVariable UUID id,
            @RequestParam String code,
            @RequestParam String nameEn,
            @RequestParam String nameAr,
            @RequestParam(defaultValue = "0") int sortOrder,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var subCategory = manageCategoryUseCase.createSubCategory(tenantId, id, code, nameEn, nameAr, sortOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryMapper.toSubCategoryResponse(subCategory));
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
