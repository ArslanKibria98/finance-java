package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreateContractTemplateRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateContractTemplateRequest;
import com.ksa.financing.product.adapter.rest.response.ContractTemplateResponse;
import com.ksa.financing.product.application.mapper.ContractTemplateMapper;
import com.ksa.financing.product.domain.model.ContractTemplate;
import com.ksa.financing.product.domain.port.in.ManageContractTemplateUseCase;
import com.ksa.financing.product.domain.port.in.ManageProductUseCase;
import com.ksa.financing.product.domain.port.in.ManageTemplateTypeUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
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

@RestController
@RequestMapping("/api/v1/contract-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contract Templates", description = "Contract template management linked to products and types")
public class ContractTemplateController {

    private final ManageContractTemplateUseCase manageContractTemplateUseCase;
    private final ManageProductUseCase manageProductUseCase;
    private final ManageTemplateTypeUseCase manageTemplateTypeUseCase;

    @SecuredEndpoint(obj = "contract-templates", act = "read")
    @GetMapping
    @Operation(summary = "List all contract templates. Optional ?search= filters by nameEn, nameAr, productName*, typeName*, language (case-insensitive LIKE).")
    public ResponseEntity<List<ContractTemplateResponse>> listAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var templates = manageContractTemplateUseCase.listAll(tenantId);
        enrichWithNames(tenantId, templates);
        return ResponseEntity.ok(filterTemplates(templates, search));
    }

    @SecuredEndpoint(obj = "contract-templates", act = "read")
    @GetMapping("/product/{productId}")
    @Operation(summary = "List contract templates by product. Optional ?search= filters by nameEn, nameAr, etc.")
    public ResponseEntity<List<ContractTemplateResponse>> listByProduct(
            @PathVariable UUID productId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var templates = manageContractTemplateUseCase.listByProduct(tenantId, productId);
        enrichWithNames(tenantId, templates);
        return ResponseEntity.ok(filterTemplates(templates, search));
    }

    @SecuredEndpoint(obj = "contract-templates", act = "read")
    @GetMapping("/type/{typeId}")
    @Operation(summary = "List contract templates by type. Optional ?search= filters by nameEn, nameAr, etc.")
    public ResponseEntity<List<ContractTemplateResponse>> listByType(
            @PathVariable UUID typeId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var templates = manageContractTemplateUseCase.listByType(tenantId, typeId);
        enrichWithNames(tenantId, templates);
        return ResponseEntity.ok(filterTemplates(templates, search));
    }

    private List<ContractTemplateResponse> filterTemplates(java.util.List<com.ksa.financing.product.domain.model.ContractTemplate> templates, String search) {
        String term = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        return templates.stream()
                .map(ContractTemplateMapper::toResponse)
                .filter(t -> term == null
                        || c(t.nameEn(), term)
                        || c(t.nameAr(), term)
                        || c(t.productNameEn(), term)
                        || c(t.productNameAr(), term)
                        || c(t.typeNameEn(), term)
                        || c(t.typeNameAr(), term)
                        || c(t.language(), term))
                .toList();
    }

    private static boolean c(String f, String t) {
        return f != null && f.toLowerCase().contains(t);
    }

    @SecuredEndpoint(obj = "contract-templates", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get contract template by ID")
    public ResponseEntity<ContractTemplateResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var template = manageContractTemplateUseCase.getById(tenantId, id);
        enrichWithNames(tenantId, List.of(template));
        return ResponseEntity.ok(ContractTemplateMapper.toResponse(template));
    }

    @SecuredEndpoint(obj = "contract-templates", act = "create")
    @PostMapping
    @Operation(summary = "Create contract template", description = "Creates a new contract template linked to a product and type")
    public ResponseEntity<ContractTemplateResponse> create(
            @Valid @RequestBody CreateContractTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating contract template nameEn={} for tenant={}", request.nameEn(), tenantId);

        var template = new ContractTemplate();
        template.setNameEn(request.nameEn());
        template.setNameAr(request.nameAr());
        template.setProductId(request.productId());
        template.setTypeId(request.typeId());
        template.setLanguage(request.language());
        template.setMessage(request.message());

        var created = manageContractTemplateUseCase.create(tenantId, template);
        enrichWithNames(tenantId, List.of(created));
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractTemplateMapper.toResponse(created));
    }

    @SecuredEndpoint(obj = "contract-templates", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update contract template")
    public ResponseEntity<ContractTemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContractTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating contract template id={} for tenant={}", id, tenantId);

        var updates = new ContractTemplate();
        updates.setNameEn(request.nameEn());
        updates.setNameAr(request.nameAr());
        updates.setProductId(request.productId());
        updates.setTypeId(request.typeId());
        updates.setLanguage(request.language());
        updates.setMessage(request.message());

        var updated = manageContractTemplateUseCase.update(tenantId, id, updates);
        enrichWithNames(tenantId, List.of(updated));
        return ResponseEntity.ok(ContractTemplateMapper.toResponse(updated));
    }

    @SecuredEndpoint(obj = "contract-templates", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contract template")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deleting contract template id={} for tenant={}", id, tenantId);

        manageContractTemplateUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private void enrichWithNames(UUID tenantId, List<ContractTemplate> templates) {
        for (var t : templates) {
            try {
                var product = manageProductUseCase.getById(tenantId, t.getProductId());
                t.setProductNameEn(product.getNameEn());
                t.setProductNameAr(product.getNameAr());
            } catch (Exception e) {
                t.setProductNameEn("Unknown");
                t.setProductNameAr("غير معروف");
            }
            try {
                var type = manageTemplateTypeUseCase.getById(tenantId, t.getTypeId());
                t.setTypeNameEn(type.getNameEn());
                t.setTypeNameAr(type.getNameAr());
            } catch (Exception e) {
                t.setTypeNameEn("Unknown");
                t.setTypeNameAr("غير معروف");
            }
        }
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
