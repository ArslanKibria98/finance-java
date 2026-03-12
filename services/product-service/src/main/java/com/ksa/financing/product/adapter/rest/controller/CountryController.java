package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.CreateCountryRequest;
import com.ksa.financing.product.adapter.rest.request.UpdateCountryRequest;
import com.ksa.financing.product.adapter.rest.response.CountryResponse;
import com.ksa.financing.product.application.mapper.CountryMapper;
import com.ksa.financing.product.domain.model.Country;
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
@Tag(name = "Countries", description = "Country reference data management for product availability and AML screening")
public class CountryController {

    private final ManageCountryUseCase manageCountryUseCase;

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping
    @Operation(summary = "List countries", description = "Returns all active countries for the tenant")
    public ResponseEntity<List<CountryResponse>> listCountries(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var countries = manageCountryUseCase.listCountries(tenantId);
        var response = countries.stream().map(CountryMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/gcc")
    @Operation(summary = "List GCC countries", description = "Returns only GCC countries for the tenant")
    public ResponseEntity<List<CountryResponse>> listGccCountries(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var countries = manageCountryUseCase.listGccCountries(tenantId);
        var response = countries.stream().map(CountryMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/arab-league")
    @Operation(summary = "List Arab League countries", description = "Returns Arab League member countries")
    public ResponseEntity<List<CountryResponse>> listArabLeagueCountries(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var countries = manageCountryUseCase.listArabLeagueCountries(tenantId);
        var response = countries.stream().map(CountryMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/sanctioned")
    @Operation(summary = "List sanctioned countries", description = "Returns sanctioned/prohibited countries for AML screening")
    public ResponseEntity<List<CountryResponse>> listSanctionedCountries(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var countries = manageCountryUseCase.listSanctionedCountries(tenantId);
        var response = countries.stream().map(CountryMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/region/{region}")
    @Operation(summary = "List countries by region", description = "Returns active countries filtered by geographic region")
    public ResponseEntity<List<CountryResponse>> listByRegion(
            @PathVariable String region,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var countries = manageCountryUseCase.listByRegion(tenantId, region);
        var response = countries.stream().map(CountryMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/risk-tier/{riskTier}")
    @Operation(summary = "List countries by risk tier", description = "Returns active countries filtered by risk tier (LOW, STANDARD, ELEVATED, HIGH, PROHIBITED)")
    public ResponseEntity<List<CountryResponse>> listByRiskTier(
            @PathVariable String riskTier,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var countries = manageCountryUseCase.listByRiskTier(tenantId, riskTier.toUpperCase());
        var response = countries.stream().map(CountryMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get country by ID", description = "Returns a single country by UUID")
    public ResponseEntity<CountryResponse> getCountry(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var country = manageCountryUseCase.getCountry(tenantId, id);
        return ResponseEntity.ok(CountryMapper.toResponse(country));
    }

    @SecuredEndpoint(obj = "countries", act = "read")
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get country by slug", description = "Returns a single country by URL-friendly slug")
    public ResponseEntity<CountryResponse> getCountryBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var country = manageCountryUseCase.getCountryBySlug(tenantId, slug);
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

        var country = mapCreateRequest(request);
        var created = manageCountryUseCase.createCountry(tenantId, country);
        return ResponseEntity.status(HttpStatus.CREATED).body(CountryMapper.toResponse(created));
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

        var updates = mapUpdateRequest(request);
        var updated = manageCountryUseCase.updateCountry(tenantId, id, updates);
        return ResponseEntity.ok(CountryMapper.toResponse(updated));
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

    private Country mapCreateRequest(CreateCountryRequest r) {
        var c = new Country();
        c.setCode(r.code());
        c.setAlpha3Code(r.alpha3Code());
        c.setNumericCode(r.numericCode());
        c.setSlug(r.slug());
        c.setNameEn(r.nameEn());
        c.setNameAr(r.nameAr());
        c.setNationalityEn(r.nationalityEn());
        c.setNationalityAr(r.nationalityAr());
        c.setDialCode(r.dialCode());
        c.setCurrencyCode(r.currencyCode());
        c.setCurrencyNameEn(r.currencyNameEn());
        c.setCurrencyNameAr(r.currencyNameAr());
        c.setFlagEmoji(r.flagEmoji());
        c.setCapitalEn(r.capitalEn());
        c.setCapitalAr(r.capitalAr());
        c.setRegion(r.region());
        c.setSubRegion(r.subRegion());
        c.setGcc(r.gcc());
        c.setArabLeague(r.arabLeague());
        c.setOicMember(r.oicMember());
        c.setSanctioned(r.sanctioned());
        c.setRiskTier(r.riskTier());
        c.setIbanRequired(r.ibanRequired());
        c.setIbanLength(r.ibanLength());
        c.setSortOrder(r.sortOrder());
        return c;
    }

    private Country mapUpdateRequest(UpdateCountryRequest r) {
        var c = new Country();
        c.setAlpha3Code(r.alpha3Code());
        c.setNumericCode(r.numericCode());
        c.setSlug(r.slug());
        c.setNameEn(r.nameEn());
        c.setNameAr(r.nameAr());
        c.setNationalityEn(r.nationalityEn());
        c.setNationalityAr(r.nationalityAr());
        c.setDialCode(r.dialCode());
        c.setCurrencyCode(r.currencyCode());
        c.setCurrencyNameEn(r.currencyNameEn());
        c.setCurrencyNameAr(r.currencyNameAr());
        c.setFlagEmoji(r.flagEmoji());
        c.setCapitalEn(r.capitalEn());
        c.setCapitalAr(r.capitalAr());
        c.setRegion(r.region());
        c.setSubRegion(r.subRegion());
        c.setGcc(r.gcc());
        c.setArabLeague(r.arabLeague());
        c.setOicMember(r.oicMember());
        c.setSanctioned(r.sanctioned());
        c.setRiskTier(r.riskTier());
        c.setIbanRequired(r.ibanRequired());
        c.setIbanLength(r.ibanLength());
        c.setSortOrder(r.sortOrder());
        c.setActive(r.active());
        return c;
    }
}
