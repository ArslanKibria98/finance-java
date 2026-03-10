package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.adapter.rest.request.CreateLoanApplicationRequest;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationResponse;
import com.ksa.financing.lending.application.mapper.LoanApplicationMapper;
import com.ksa.financing.lending.domain.model.ShariaStructure;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/loan-applications")
@RequiredArgsConstructor
@Tag(name = "Loan Applications", description = "Loan application management endpoints")
public class LoanApplicationController {

    private final ManageLoanApplicationUseCase useCase;
    private final LoanApplicationMapper mapper;

    @SecuredEndpoint(obj = "loan-applications", act = "create")
    @PostMapping
    @Operation(summary = "Create a new loan application")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Application created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<LoanApplicationResponse> createApplication(
            @Valid @RequestBody CreateLoanApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Creating loan application. CorrelationId: {}", correlationId);

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        var command = new ManageLoanApplicationUseCase.CreateApplicationCommand(
                tenantId,
                UUID.fromString(request.customerId()),
                UUID.fromString(request.productId()),
                request.productCode(),
                ShariaStructure.valueOf(request.shariaStructure()),
                request.requestedAmount(),
                request.requestedTenureMonths(),
                request.partnerId() != null ? UUID.fromString(request.partnerId()) : null,
                request.leadId() != null ? UUID.fromString(request.leadId()) : null,
                userId,
                request.idempotencyKey()
        );

        var aggregate = useCase.createApplication(command);
        var dto = mapper.toDto(aggregate);
        var response = LoanApplicationResponse.from(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{applicationId}/submit")
    @Operation(summary = "Submit a loan application for processing")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application submitted"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    public ResponseEntity<LoanApplicationResponse> submitApplication(
            @PathVariable String applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Submitting loan application: {}. CorrelationId: {}", applicationId, correlationId);

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        var command = new ManageLoanApplicationUseCase.SubmitApplicationCommand(
                tenantId,
                UUID.fromString(applicationId),
                userId
        );

        var aggregate = useCase.submitApplication(command);
        var dto = mapper.toDto(aggregate);
        var response = LoanApplicationResponse.from(dto);

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/{applicationId}")
    @Operation(summary = "Get a loan application by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application found"),
        @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<LoanApplicationResponse> getApplication(
            @PathVariable String applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);

        var aggregate = useCase.getApplication(tenantId, UUID.fromString(applicationId));
        var dto = mapper.toDto(aggregate);
        var response = LoanApplicationResponse.from(dto);

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping
    @Operation(summary = "List all loan applications for tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Applications listed")
    })
    public ResponseEntity<List<LoanApplicationResponse>> listApplications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);

        var aggregates = useCase.listApplications(tenantId);
        var responses = mapper.toDtos(aggregates).stream()
                .map(LoanApplicationResponse::from)
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

    private UUID extractUserId(Jwt jwt) {
        var subject = jwt.getSubject();
        if (subject == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No subject claim found in JWT token");
        }
        return UUID.fromString(subject);
    }
}
