package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.ledger.application.dto.ReconciliationReportResponse;
import com.ksa.financing.ledger.application.mapper.JournalEntryMapper;
import com.ksa.financing.ledger.domain.port.in.RunReconciliationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for GL reconciliation operations.
 * All endpoints protected by Casbin ABAC via @SecuredEndpoint.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reconciliation", description = "GL reconciliation between internal ledger and Fineract")
public class ReconciliationController {

    private final RunReconciliationUseCase runReconciliationUseCase;
    private final JournalEntryMapper mapper;

    @SecuredEndpoint(obj = "ledger.reconciliation", act = "manage")
    @PostMapping("/run")
    @Operation(
            summary = "Run GL reconciliation",
            description = "Compare internal account balances with Fineract. Reports discrepancies."
    )
    @ApiResponse(responseCode = "200", description = "Reconciliation completed")
    public ResponseEntity<ReconciliationReportResponse> runReconciliation(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reconciliationDate = date != null ? date : LocalDate.now().minusDays(1);

        log.info("Running reconciliation: tenant={} date={}", tenantId, reconciliationDate);

        RunReconciliationUseCase.ReconciliationSummary summary =
                runReconciliationUseCase.run(tenantId, reconciliationDate);

        return ResponseEntity.ok(mapper.toReconResponse(summary));
    }

    @SecuredEndpoint(obj = "ledger.reconciliation", act = "read")
    @GetMapping("/report")
    @Operation(
            summary = "Get reconciliation report",
            description = "Returns the reconciliation report for the given date"
    )
    @ApiResponse(responseCode = "200", description = "Report retrieved")
    public ResponseEntity<ReconciliationReportResponse> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Fetching reconciliation report: tenant={} date={}", tenantId, date);

        // Run on-demand (in production this would read from gl_reconciliation_records)
        RunReconciliationUseCase.ReconciliationSummary summary =
                runReconciliationUseCase.run(tenantId, date);

        return ResponseEntity.ok(mapper.toReconResponse(summary));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
