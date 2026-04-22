package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.adapter.rest.request.CheckEligibilityRequest;
import com.ksa.financing.lending.adapter.rest.request.FinanceCalculatorRequest;
import com.ksa.financing.lending.adapter.rest.request.SuggestProductsRequest;
import com.ksa.financing.lending.adapter.rest.response.CheckEligibilityResponse;
import com.ksa.financing.lending.adapter.rest.response.FinanceCalculatorResponse;
import com.ksa.financing.lending.adapter.rest.response.SuggestProductsResponse;
import com.ksa.financing.lending.domain.port.in.CalculateFinanceUseCase;
import com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase;
import com.ksa.financing.lending.domain.port.in.SuggestProductsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * BRD UC#01 — Finance Calculator + Pre-Qualification.
 * Stateless endpoints: no DB writes, no workflow.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Tag(name = "Finance Calculator", description = "BRD UC#01 — Finance calculation and eligibility check")
public class FinanceCalculatorController {

    private final CalculateFinanceUseCase calculateFinanceUseCase;
    private final CheckEligibilityUseCase checkEligibilityUseCase;
    private final SuggestProductsUseCase suggestProductsUseCase;

    @SecuredEndpoint(obj = "finance.calculator", act = "read")
    @PostMapping("/calculator")
    @Operation(summary = "Calculate finance details (BRD UC#01)")
    public ResponseEntity<FinanceCalculatorResponse> calculate(
            @Valid @RequestBody FinanceCalculatorRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        var result = calculateFinanceUseCase.calculate(
                new CalculateFinanceUseCase.CalculateFinanceCommand(
                        tenantId,
                        request.amount(),
                        request.tenureMonths(),
                        request.totalIncome(),
                        request.productId()
                )
        );

        if (result.hasErrors()) {
            return ResponseEntity.ok(FinanceCalculatorResponse.rejected(result.errors()));
        }
        return ResponseEntity.ok(FinanceCalculatorResponse.from(result));
    }

    @SecuredEndpoint(obj = "finance.eligibility", act = "check")
    @PostMapping("/check-eligibility")
    @Operation(summary = "Check eligibility / pre-qualification (BRD Steps 4-10)")
    public ResponseEntity<CheckEligibilityResponse> checkEligibility(
            @Valid @RequestBody CheckEligibilityRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        var result = checkEligibilityUseCase.checkEligibility(
                new CheckEligibilityUseCase.CheckEligibilityCommand(
                        tenantId,
                        request.amount(),
                        request.tenureMonths(),
                        request.salary(),
                        request.liabilities(),
                        request.adultDependents(),
                        request.childDependents(),
                        request.foodGroceries(),
                        request.utilities(),
                        request.healthcare(),
                        request.communication(),
                        request.housingRent(),
                        request.clothingEssentials(),
                        request.education(),
                        request.transportation(),
                        request.productId()
                )
        );

        return ResponseEntity.ok(CheckEligibilityResponse.from(result));
    }

    @SecuredEndpoint(obj = "finance.suggestions", act = "read")
    @PostMapping("/suggest-products")
    @Operation(summary = "Suggest eligible products based on financial info (no productId required)")
    public ResponseEntity<SuggestProductsResponse> suggestProducts(
            @Valid @RequestBody SuggestProductsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        var result = suggestProductsUseCase.suggestProducts(
                new SuggestProductsUseCase.SuggestProductsCommand(
                        tenantId,
                        jwt.getTokenValue(),
                        request.salary(),
                        request.liabilities(),
                        request.adultDependents(),
                        request.childDependents(),
                        request.foodGroceries(),
                        request.utilities(),
                        request.healthcare(),
                        request.communication(),
                        request.housingRent(),
                        request.clothingEssentials(),
                        request.education(),
                        request.transportation()
                )
        );

        return ResponseEntity.ok(SuggestProductsResponse.from(result));
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
