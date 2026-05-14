package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.CreateProviderApiRequest;
import com.ksa.financing.middleware.application.dto.ProviderApiResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderApiRequest;
import com.ksa.financing.middleware.domain.port.in.ManageProviderApiUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider-apis")
@RequiredArgsConstructor
public class ProviderApiController {

    private final ManageProviderApiUseCase manageProviderApiUseCase;
    private final JdbcTemplate jdbcTemplate;

    /** Admin-only: update only the cost without touching other fields. */
    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @PatchMapping("/{id}/cost")
    public ResponseEntity<Map<String, Object>> updateCost(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        if (!body.containsKey("costPerCall")) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "costPerCall is required");
        }
        BigDecimal newCost = new BigDecimal(body.get("costPerCall").toString());
        if (newCost.signum() < 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "costPerCall must be >= 0");
        }
        String currency = body.containsKey("costCurrency") ? body.get("costCurrency").toString() : "SAR";

        int affected = jdbcTemplate.update(
                "UPDATE provider_apis SET cost_per_call = ?, cost_currency = ?, updated_at = NOW() " +
                        "WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL",
                newCost, currency, id, tenantId);
        if (affected == 0) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "API not found or already deleted: " + id);
        }
        return ResponseEntity.ok(Map.of("id", id, "costPerCall", newCost, "costCurrency", currency));
    }

    /** Admin-only: update cost by API code (convenience for ops console). */
    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @PutMapping("/by-code/{code}/cost")
    public ResponseEntity<Map<String, Object>> updateCostByCode(
            @PathVariable String code,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        if (!body.containsKey("costPerCall")) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "costPerCall is required");
        }
        BigDecimal newCost = new BigDecimal(body.get("costPerCall").toString());
        if (newCost.signum() < 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "costPerCall must be >= 0");
        }
        String currency = body.containsKey("costCurrency") ? body.get("costCurrency").toString() : "SAR";

        int affected = jdbcTemplate.update(
                "UPDATE provider_apis SET cost_per_call = ?, cost_currency = ?, updated_at = NOW() " +
                        "WHERE code = ? AND tenant_id = ? AND deleted_at IS NULL",
                newCost, currency, code, tenantId);
        if (affected == 0) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "API not found for code: " + code);
        }
        return ResponseEntity.ok(Map.of("code", code, "costPerCall", newCost, "costCurrency", currency));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @PostMapping
    public ResponseEntity<ProviderApiResponse> create(@Valid @RequestBody CreateProviderApiRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var createdBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageProviderApiUseCase.create(tenantId, request, createdBy));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "read")
    @GetMapping("/{id}")
    public ResponseEntity<ProviderApiResponse> getById(@PathVariable UUID id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "read")
    @GetMapping("/by-provider/{providerId}")
    public ResponseEntity<List<ProviderApiResponse>> listByProvider(@PathVariable UUID providerId,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.listByProvider(tenantId, providerId));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "read")
    @GetMapping
    public ResponseEntity<List<ProviderApiResponse>> listAll(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.listAll(tenantId));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @PutMapping("/{id}")
    public ResponseEntity<ProviderApiResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateProviderApiRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.update(tenantId, id, request));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageProviderApiUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaim("tenant_id");
        if (tenantClaim == null) {
            throw new com.ksa.financing.infra.exception.BusinessException(
                    "COMMON.AUTH.INVALID_CREDENTIALS",
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim.toString());
    }
}
