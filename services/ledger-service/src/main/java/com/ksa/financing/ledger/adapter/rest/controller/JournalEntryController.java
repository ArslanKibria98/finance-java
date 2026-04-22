package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.application.dto.JournalEntryResponse;
import com.ksa.financing.ledger.application.dto.JournalLineDto;
import com.ksa.financing.ledger.application.dto.PostJournalEntryRequest;
import com.ksa.financing.ledger.application.mapper.JournalEntryMapper;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.domain.port.out.JournalEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Journal Entry operations.
 * All endpoints protected by Casbin ABAC via @SecuredEndpoint.
 */
@RestController
@RequestMapping("/api/v1/journal-entries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Journal Entries", description = "Post and query double-entry journal entries")
public class JournalEntryController {

    private final PostJournalEntryUseCase postJournalEntryUseCase;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryMapper mapper;

    @SecuredEndpoint(obj = "ledger.entries", act = "create")
    @PostMapping
    @Operation(summary = "Post a journal entry", description = "Post a balanced double-entry journal entry to the ledger")
    @ApiResponse(responseCode = "201", description = "Journal entry posted")
    @ApiResponse(responseCode = "400", description = "Unbalanced or invalid entry")
    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
    public ResponseEntity<JournalEntryResponse> postEntry(
            @Valid @RequestBody PostJournalEntryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());

        log.info("Posting journal entry: tenant={} reference={}/{} idempotencyKey={}",
                tenantId, request.referenceType(), request.referenceId(), request.idempotencyKey());

        // Map DTO lines to domain JournalLine objects
        List<JournalLine> domainLines = mapToJournalLines(tenantId, request.lines());

        PostJournalEntryUseCase.PostJournalEntryCommand command =
                new PostJournalEntryUseCase.PostJournalEntryCommand(
                        tenantId,
                        request.entryDate(),
                        request.referenceType(),
                        request.referenceId(),
                        request.transactionType(),
                        request.description(),
                        domainLines,
                        request.idempotencyKey(),
                        userId
                );

        JournalEntryAggregate entry = postJournalEntryUseCase.post(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(entry));
    }

    @SecuredEndpoint(obj = "ledger.entries", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get journal entry by ID")
    @ApiResponse(responseCode = "200", description = "Entry found")
    @ApiResponse(responseCode = "404", description = "Entry not found")
    public ResponseEntity<JournalEntryResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        JournalEntryAggregate entry = journalEntryRepository
                .findById(tenantId, JournalEntryId.of(id))
                .orElseThrow(() -> NotFoundException.forEntity("JournalEntry", id.toString()));

        return ResponseEntity.ok(mapper.toResponse(entry));
    }

    @SecuredEndpoint(obj = "ledger.entries", act = "read")
    @GetMapping("/by-reference")
    @Operation(summary = "Get journal entries by reference", description = "Query entries by reference type and ID (e.g. LOAN, DISBURSEMENT_ABC)")
    public ResponseEntity<List<JournalEntryResponse>> getByReference(
            @RequestParam String referenceType,
            @RequestParam UUID referenceId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        List<JournalEntryAggregate> entries = journalEntryRepository
                .findByReference(tenantId, referenceType, referenceId);

        return ResponseEntity.ok(entries.stream().map(mapper::toResponse).toList());
    }

    @SecuredEndpoint(obj = "ledger.entries", act = "read")
    @GetMapping("/by-date")
    @Operation(summary = "Get journal entries by date")
    public ResponseEntity<List<JournalEntryResponse>> getByDate(
            @RequestParam LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        List<JournalEntryAggregate> entries = journalEntryRepository.findByDate(tenantId, date);
        return ResponseEntity.ok(entries.stream().map(mapper::toResponse).toList());
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

    /**
     * Map request DTOs to domain JournalLine value objects.
     * Resolves account code → AccountId via the account repository.
     */
    private List<JournalLine> mapToJournalLines(UUID tenantId, List<JournalLineDto> lineDtos) {
        List<JournalLine> lines = new ArrayList<>();
        int lineNumber = 1;

        for (JournalLineDto dto : lineDtos) {
            var account = accountRepository.findByCode(tenantId, dto.accountCode())
                    .orElseThrow(() -> NotFoundException.forEntity("Account", dto.accountCode()));

            BigDecimal debit = dto.debitAmount() != null ? dto.debitAmount() : BigDecimal.ZERO;
            BigDecimal credit = dto.creditAmount() != null ? dto.creditAmount() : BigDecimal.ZERO;

            lines.add(new JournalLine(account.getId(), debit, credit, dto.description(), lineNumber++));
        }

        return lines;
    }
}
