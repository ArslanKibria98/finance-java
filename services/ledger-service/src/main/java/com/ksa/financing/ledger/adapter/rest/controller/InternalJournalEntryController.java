package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.application.dto.JournalEntryResponse;
import com.ksa.financing.ledger.application.dto.JournalLineDto;
import com.ksa.financing.ledger.application.dto.PostJournalEntryRequest;
import com.ksa.financing.ledger.application.mapper.JournalEntryMapper;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service-to-service journal posting (lending-service, etc.) without JWT.
 * Secured by network / mesh; same business rules as {@link JournalEntryController#postEntry}.
 */
@RestController
@RequestMapping("/internal/v1/journal-entries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Journal Entries", description = "Post journal entries from trusted backend services")
public class InternalJournalEntryController {

    private final PostJournalEntryUseCase postJournalEntryUseCase;
    private final AccountRepository accountRepository;
    private final JournalEntryMapper mapper;

    @Value("${app.internal-journal.default-created-by-user-id:}")
    private String defaultCreatedByUserId;

    @PostMapping
    @Operation(summary = "Post a journal entry (internal)")
    public ResponseEntity<JournalEntryResponse> postEntry(
            @Valid @RequestBody PostJournalEntryRequest request,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader,
            @RequestHeader(value = "X-Created-By", required = false) String createdByHeader,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService) {

        UUID tenantId = parseTenantId(tenantIdHeader);
        UUID userId = resolveCreatedBy(createdByHeader);

        log.info("Internal journal post: caller={} tenant={} reference={}/{} idempotencyKey={}",
                callerService, tenantId, request.referenceType(), request.referenceId(), request.idempotencyKey());

        List<JournalLine> domainLines = mapToJournalLines(tenantId, request.lines());

        var command = new PostJournalEntryUseCase.PostJournalEntryCommand(
                tenantId,
                request.entryDate(),
                request.referenceType(),
                request.referenceId(),
                request.transactionType(),
                request.description(),
                domainLines,
                request.idempotencyKey(),
                userId);

        var entry = postJournalEntryUseCase.post(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(entry));
    }

    private UUID resolveCreatedBy(String createdByHeader) {
        if (StringUtils.hasText(createdByHeader)) {
            try {
                return UUID.fromString(createdByHeader.trim());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST, "Invalid X-Created-By UUID");
            }
        }
        if (StringUtils.hasText(defaultCreatedByUserId)) {
            try {
                return UUID.fromString(defaultCreatedByUserId.trim());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(
                        ErrorCodes.BAD_REQUEST,
                        "Misconfigured app.internal-journal.default-created-by-user-id");
            }
        }
        throw new BusinessException(
                ErrorCodes.BAD_REQUEST,
                "X-Created-By header or app.internal-journal.default-created-by-user-id is required");
    }

    private static UUID parseTenantId(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "X-Tenant-Id header is required");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Invalid X-Tenant-Id format");
        }
    }

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
