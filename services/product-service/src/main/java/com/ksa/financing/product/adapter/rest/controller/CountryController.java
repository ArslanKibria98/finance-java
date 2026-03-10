package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreateCountryRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateCountryRequest;
import com.ksa.financing.product.adapter.rest.response.CountryResponse;
import com.ksa.financing.product.application.mapper.CountryMapper;
import com.ksa.financing.product.domain.port.in.ManageCountryUseCase;
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
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Countries", description = "Country reference data management for product availability")
public class CountryController {

    private final ManageCountryUseCase manageCountryUseCase;

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping
    @Operation(summary = "List countries", description = "Returns all active countries for the tenant")
    public ResponseEntity<List<CountryResponse>> listCountries(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing countries for tenantId: {}", tenantId);

        var countries = manageCountryUseCase.listCountries(tenantId);
        var response = countries.stream()
                .map(CountryMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/gcc")
    @Operation(summary = "List GCC countries", description = "Returns only GCC countries for the tenant")
    public ResponseEntity<List<CountryResponse>> listGccCountries(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing GCC countries for tenantId: {}", tenantId);

        var countries = manageCountryUseCase.listGccCountries(tenantId);
        var response = countries.stream()
                .map(CountryMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get country", description = "Returns a single country by ID")
    public ResponseEntity<CountryResponse> getCountry(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Getting country id: {} for tenantId: {}", id, tenantId);

        var country = manageCountryUseCase.getCountry(tenantId, id);
        return ResponseEntity.ok(CountryMapper.toResponse(country));
    }

    @SecuredEndpoint(obj = "countries", act = "create")
    @PostMapping
    @Operation(summary = "Create country", description = "Creates a new country entry")
    public ResponseEntity<CountryResponse> createCountry(
            @Valid @RequestBody CreateCountryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating country code={} for tenantId={}", request.code(), tenantId);

        var country = manageCountryUseCase.createCountry(
                tenantId, request.code(), request.nameEn(), request.nameAr(),
                request.dialCode(), request.currencyCode(), request.gcc(), request.sortOrder());

        return ResponseEntity.status(HttpStatus.CREATED).body(CountryMapper.toResponse(country));
    }

    @SecuredEndpoint(obj = "countries", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update country", description = "Updates an existing country entry")
    public ResponseEntity<CountryResponse> updateCountry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCountryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating country id={} for tenantId={}", id, tenantId);

        var country = manageCountryUseCase.updateCountry(
                tenantId, id, request.nameEn(), request.nameAr(),
                request.dialCode(), request.currencyCode(), request.gcc(),
                request.sortOrder(), request.active());

        return ResponseEntity.ok(CountryMapper.toResponse(country));
    }

    @SecuredEndpoint(obj = "countries", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete country", description = "Deletes a country entry")
    public ResponseEntity<Void> deleteCountry(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Deleting country id={} for tenantId={}", id, tenantId);

        manageCountryUseCase.deleteCountry(tenantId, id);
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
