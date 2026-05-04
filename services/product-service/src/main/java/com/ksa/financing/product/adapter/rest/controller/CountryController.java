package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.application.mapper.CountryMapper;
import com.ksa.financing.product.adapter.rest.response.CountryResponse;
import com.ksa.financing.product.domain.port.in.ManageCountryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Tag(name = "Countries", description = "Endpoints for managing regional and international countries")
public class CountryController {

    private final ManageCountryUseCase manageCountryUseCase;

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping
    @Operation(summary = "List all countries")
    public PageResponse<CountryResponse> listCountries(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageCountryUseCase.listCountries(tenantId, query).map(CountryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/gcc")
    @Operation(summary = "List GCC countries")
    public PageResponse<CountryResponse> listGccCountries(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageCountryUseCase.listGccCountries(tenantId, query).map(CountryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/arab-league")
    @Operation(summary = "List Arab League countries")
    public PageResponse<CountryResponse> listArabLeagueCountries(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageCountryUseCase.listArabLeagueCountries(tenantId, query).map(CountryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/sanctioned")
    @Operation(summary = "List sanctioned countries")
    public PageResponse<CountryResponse> listSanctionedCountries(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageCountryUseCase.listSanctionedCountries(tenantId, query).map(CountryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/region/{region}")
    @Operation(summary = "List countries by region")
    public PageResponse<CountryResponse> listByRegion(
            @PathVariable String region,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageCountryUseCase.listByRegion(tenantId, region, query).map(CountryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/risk-tier/{riskTier}")
    @Operation(summary = "List countries by risk tier")
    public PageResponse<CountryResponse> listByRiskTier(
            @PathVariable String riskTier,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageCountryUseCase.listByRiskTier(tenantId, riskTier, query).map(CountryMapper::toResponse);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get country by ID")
    public ResponseEntity<CountryResponse> getCountry(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(CountryMapper.toResponse(manageCountryUseCase.getCountry(tenantId, id)));
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get country by slug")
    public ResponseEntity<CountryResponse> getCountryBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(CountryMapper.toResponse(manageCountryUseCase.getCountryBySlug(tenantId, slug)));
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
