package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.adapter.rest.request.PostSimpleAccountMovementRequest;
import com.ksa.financing.ledger.application.dto.LedgerReportResponse;
import com.ksa.financing.ledger.application.dto.PostSimpleAccountMovementResponse;
import com.ksa.financing.ledger.application.mapper.JournalEntryMapper;
import com.ksa.financing.ledger.application.service.LedgerReportService;
import com.ksa.financing.ledger.domain.port.in.PostSimpleAccountMovementUseCase;
import com.ksa.financing.ledger.domain.port.in.PostSimpleAccountMovementUseCase.SimpleAccountMovementCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Simplified journal posting: one COA line + offset account from configuration.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ledger/admin-movements")
@RequiredArgsConstructor
@Tag(name = "Admin ledger movements", description = "Post balanced movements against a single COA account")
public class AdminAccountMovementController {

    private final PostSimpleAccountMovementUseCase postSimpleAccountMovementUseCase;
    private final JournalEntryMapper journalEntryMapper;
    private final LedgerReportService ledgerReportService;

    @SecuredEndpoint(obj = "ledger.admin-movements", act = "create")
    @PostMapping
    @Operation(summary = "Post a simple COA movement",
            description = "Creates a balanced journal entry: debit/credit on the selected account and the opposite on "
                    + "the configured offset GL account (LEDGER_ADMIN_ADJUSTMENT_OFFSET_ACCOUNT_CODE).")
    public ResponseEntity<PostSimpleAccountMovementResponse> postMovement(
            @Valid @RequestBody PostSimpleAccountMovementRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        validateAccountSelector(request);

        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new SimpleAccountMovementCommand(
                tenantId,
                request.accountCode(),
                request.accountId(),
                request.movement(),
                request.amount(),
                request.entryDate(),
                request.idempotencyKey(),
                request.description(),
                request.referenceType(),
                request.referenceId(),
                userId
        );

        var result = postSimpleAccountMovementUseCase.post(command);

        LedgerReportResponse.AccountLedger ledgerSnapshot = null;
        if (request.returnLedgerSnapshot()) {
            var report = ledgerReportService.generate(
                    tenantId,
                    request.entryDate(),
                    request.entryDate(),
                    null,
                    result.targetAccountId(),
                    new PageQuery(0, 100, null, null, null)
            );
            if (!report.accounts().isEmpty()) {
                ledgerSnapshot = report.accounts().getFirst();
            }
        }

        var response = PostSimpleAccountMovementResponse.builder()
                .journalEntry(journalEntryMapper.toResponse(result.journalEntry()))
                .ledgerSnapshot(ledgerSnapshot)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void validateAccountSelector(PostSimpleAccountMovementRequest request) {
        boolean codePresent = StringUtils.hasText(request.accountCode());
        boolean idPresent = request.accountId() != null;
        if (codePresent == idPresent) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Exactly one of accountCode or accountId must be provided");
        }
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
