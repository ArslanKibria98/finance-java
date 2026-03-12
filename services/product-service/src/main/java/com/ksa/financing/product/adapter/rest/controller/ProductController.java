package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreateProductRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateBasicInfoRequest;
import com.ksa.financing.product.adapter.rest.response.ProductResponse;
import com.ksa.financing.product.adapter.rest.response.ProductSummaryResponse;
import com.ksa.financing.product.application.mapper.ProductMapper;
import com.ksa.financing.product.domain.port.in.ManageProductUseCase;
import com.ksa.financing.product.domain.port.in.ManageProductUseCase.CreateProductCommand;
import com.ksa.financing.product.domain.port.in.ManageProductUseCase.UpdateBasicInfoCommand;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Islamic financing product lifecycle management")
public class ProductController {

    private final ManageProductUseCase manageProductUseCase;

    @SecuredEndpoint(obj = "products", act = "create")
    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new Islamic financing product in DRAFT status")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        log.info("Creating product with code: {} for tenantId: {}", request.productCode(), tenantId);

        var command = new CreateProductCommand(
                tenantId,
                request.productCode(),
                request.nameEn(),
                request.nameAr(),
                request.descriptionEn(),
                request.descriptionAr(),
                request.shortDescriptionEn(),
                request.shortDescriptionAr(),
                request.productType(),
                request.targetSegment(),
                request.masterCategoryId(),
                request.subCategoryId(),
                request.templateId(),
                request.notificationEmail(),
                request.customerTypes(),
                request.involvesCommodity(),
                request.setupMethod(),
                request.shariaStructure(),
                request.minAmount(),
                request.maxAmount(),
                request.minTenureMonths(),
                request.maxTenureMonths(),
                request.allowedTenures(),
                request.baseProfitRate(),
                request.rateType(),
                request.repaymentFrequency(),
                request.gracePeriodDays(),
                request.earlySettlementAllowed(),
                request.countryId(),
                userId
        );

        var product = manageProductUseCase.create(command);
        var response = ProductMapper.toResponse(product);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @SecuredEndpoint(obj = "products", act = "read")
    @GetMapping
    @Operation(summary = "List products", description = "Returns all products for the tenant")
    public ResponseEntity<List<ProductSummaryResponse>> listProducts(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing products for tenantId: {}", tenantId);

        var products = manageProductUseCase.listByTenant(tenantId);
        var response = products.stream()
                .map(ProductMapper::toSummaryResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "products", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get product", description = "Returns a single product by ID")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Getting product id: {} for tenantId: {}", id, tenantId);

        var product = manageProductUseCase.getById(tenantId, id);
        var response = ProductMapper.toResponse(product);

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "products", act = "update")
    @PutMapping("/{id}/basic-info")
    @Operation(summary = "Update basic info", description = "Updates product basic information (wizard step 1)")
    public ResponseEntity<ProductResponse> updateBasicInfo(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBasicInfoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        log.info("Updating basic info for product id: {} tenantId: {}", id, tenantId);

        var command = new UpdateBasicInfoCommand(
                request.nameEn(),
                request.nameAr(),
                request.descriptionEn(),
                request.descriptionAr(),
                request.shortDescriptionEn(),
                request.shortDescriptionAr(),
                request.notificationEmail(),
                request.customerTypes(),
                request.involvesCommodity(),
                request.logoUrl(),
                request.countryId(),
                userId
        );

        var product = manageProductUseCase.updateBasicInfo(tenantId, id, command);
        var response = ProductMapper.toResponse(product);

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "products", act = "manage")
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate product", description = "Validates product, creates in Fineract, and activates the product via Temporal workflow")
    public ResponseEntity<java.util.Map<String, Object>> activateProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Activating product id: {} for tenantId: {}", id, tenantId);

        var result = manageProductUseCase.activate(tenantId, id);

        var response = new java.util.LinkedHashMap<String, Object>();
        response.put("productId", result.productId());
        response.put("status", result.status());
        response.put("success", result.success());
        response.put("workflowId", result.workflowId());
        if (result.fineractProductId() != null) {
            response.put("fineractProductId", result.fineractProductId());
        }
        if (result.failureReason() != null) {
            response.put("failureReason", result.failureReason());
        }

        if (result.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }

    @SecuredEndpoint(obj = "products", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate product", description = "Deactivates an ACTIVE product")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deactivating product id: {} for tenantId: {}", id, tenantId);

        manageProductUseCase.deactivate(tenantId, id);

        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "products", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft-deletes a product (sets deletedAt timestamp)")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Soft-deleting product id: {} for tenantId: {}", id, tenantId);

        manageProductUseCase.softDelete(tenantId, id);

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

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
