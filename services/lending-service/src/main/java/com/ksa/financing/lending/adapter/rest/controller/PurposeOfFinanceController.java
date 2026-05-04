package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;
import com.ksa.financing.lending.domain.port.in.ManagePurposeOfFinanceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/purpose-of-finance")
@RequiredArgsConstructor
@Tag(name = "Purpose of Finance", description = "CRUD for purpose of finance reference data")
public class PurposeOfFinanceController {

    private final ManagePurposeOfFinanceUseCase useCase;

    @SecuredEndpoint(obj = "purpose-of-finance", act = "create")
    @PostMapping
    @Operation(summary = "Create a new purpose of finance")
    public ResponseEntity<PurposeOfFinanceResponse> create(
            @Valid @RequestBody CreatePurposeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        var entry = useCase.create(tenantId, request.code(), request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.sortOrder(), userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(PurposeOfFinanceResponse.from(entry));
    }

    @SecuredEndpoint(obj = "purpose-of-finance", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update a purpose of finance")
    public ResponseEntity<PurposeOfFinanceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePurposeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var entry = useCase.update(tenantId, id, request.nameEn(), request.nameAr(),
                request.descriptionEn(), request.descriptionAr(), request.sortOrder(), request.active());

        return ResponseEntity.ok(PurposeOfFinanceResponse.from(entry));
    }

    @SecuredEndpoint(obj = "purpose-of-finance", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get a purpose of finance by ID")
    public ResponseEntity<PurposeOfFinanceResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var entry = useCase.getById(tenantId, id);
        return ResponseEntity.ok(PurposeOfFinanceResponse.from(entry));
    }

    @SecuredEndpoint(obj = "purpose-of-finance", act = "read")
    @GetMapping
    @Operation(summary = "List active purposes of finance")
    public PageResponse<PurposeOfFinanceResponse> listActive(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        PageResponse<PurposeOfFinanceEntry> entries = activeOnly
                ? useCase.listActive(tenantId, pageQuery)
                : useCase.listAll(tenantId, pageQuery);
        return entries.map(PurposeOfFinanceResponse::from);
    }

    @SecuredEndpoint(obj = "purpose-of-finance", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a purpose of finance")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        useCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ══════════ DTOs ══════════

    public record CreatePurposeRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "English name is required") String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            int sortOrder
    ) {}

    public record UpdatePurposeRequest(
            @NotBlank(message = "English name is required") String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            int sortOrder,
            boolean active
    ) {}

    public record PurposeOfFinanceResponse(
            UUID id,
            String code,
            String nameEn,
            String nameAr,
            String descriptionEn,
            String descriptionAr,
            boolean active,
            int sortOrder
    ) {
        public static PurposeOfFinanceResponse from(PurposeOfFinanceEntry entry) {
            return new PurposeOfFinanceResponse(
                    entry.getId(), entry.getCode(), entry.getNameEn(), entry.getNameAr(),
                    entry.getDescriptionEn(), entry.getDescriptionAr(),
                    entry.isActive(), entry.getSortOrder()
            );
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

    private UUID extractUserId(Jwt jwt) {
        var subject = jwt.getSubject();
        if (subject == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No subject claim found in JWT token");
        }
        return UUID.fromString(subject);
    }
}
