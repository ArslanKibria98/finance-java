package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.adapter.rest.response.LoanResponse;
import com.ksa.financing.lending.application.mapper.LoanMapper;
import com.ksa.financing.lending.domain.port.in.ManageLoanUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Active loan management endpoints")
public class LoanController {

    private final ManageLoanUseCase useCase;
    private final LoanMapper mapper;

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/{loanId}")
    @Operation(summary = "Get a loan by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponse> getLoan(
            @PathVariable String loanId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoan(tenantId, UUID.fromString(loanId));
        var response = LoanResponse.from(mapper.toDto(loan));

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/by-number/{loanNumber}")
    @Operation(summary = "Get a loan by loan number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponse> getLoanByNumber(
            @PathVariable String loanNumber,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoanByNumber(tenantId, loanNumber);
        var response = LoanResponse.from(mapper.toDto(loan));

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List loans by customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loans listed")
    })
    public ResponseEntity<List<LoanResponse>> listLoansByCustomer(
            @PathVariable String customerId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loans = useCase.listLoansByCustomer(tenantId, UUID.fromString(customerId));
        var responses = mapper.toDtos(loans).stream()
                .map(LoanResponse::from)
                .toList();

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(responses);
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
