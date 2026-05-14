package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.DayBookReportResponse;
import com.ksa.financing.ledger.application.dto.DayBookReportResponse.DayBookEntry;
import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates the Day Book Report — chronological listing of every debit and credit
 * line posted on a given date, one row per journal line.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DayBookReportService {

    private final JpaJournalEntryRepository journalEntryRepository;
    private final JpaAccountRepository accountRepository;

    public DayBookReportResponse generate(UUID tenantId, LocalDate reportDate, PageQuery pageQuery) {
        log.info("Generating day book report: tenant={}, date={}, search={}, page={}",
                tenantId, reportDate, pageQuery.search(), pageQuery.page());

        Page<JournalEntryJpaEntity> entryPage = journalEntryRepository
                .findForDayBook(tenantId, reportDate, toLikePattern(pageQuery.search()), pageQuery.toPageable());
        return buildResponse(reportDate, entryPage, tenantId);
    }

    public DayBookReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate, PageQuery pageQuery) {
        log.info("Generating day book report: tenant={}, from={}, to={}, search={}, page={}",
                tenantId, fromDate, toDate, pageQuery.search(), pageQuery.page());

        Page<JournalEntryJpaEntity> entryPage = journalEntryRepository
                .findForDayBookRange(tenantId, fromDate, toDate, toLikePattern(pageQuery.search()), pageQuery.toPageable());
        return buildResponse(fromDate, entryPage, tenantId);
    }

    private String toLikePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return "%" + raw.trim().toLowerCase() + "%";
    }

    private DayBookReportResponse buildResponse(LocalDate reportDate,
                                                Page<JournalEntryJpaEntity> entryPage,
                                                UUID tenantId) {
        List<JournalEntryJpaEntity> entries = new ArrayList<>(entryPage.getContent());
        entries.sort(Comparator
                .comparing(JournalEntryJpaEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(JournalEntryJpaEntity::getEntryNumber, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<UUID, AccountJpaEntity> accountCache = new HashMap<>();
        List<DayBookEntry> dayBookEntries = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntryJpaEntity entry : entries) {
            List<JournalLineJpaEntity> lines = new ArrayList<>(entry.getLines());
            lines.sort(Comparator.comparingInt(JournalLineJpaEntity::getLineNumber));

            for (JournalLineJpaEntity line : lines) {
                AccountJpaEntity account = accountCache.computeIfAbsent(
                        line.getAccountId(),
                        id -> accountRepository.findByTenantIdAndId(tenantId, id).orElse(null));

                BigDecimal debit = nullSafe(line.getDebitAmount());
                BigDecimal credit = nullSafe(line.getCreditAmount());
                totalDebits = totalDebits.add(debit);
                totalCredits = totalCredits.add(credit);

                dayBookEntries.add(DayBookEntry.builder()
                        .postedAt(entry.getCreatedAt())
                        .entryId(entry.getId())
                        .voucherNumber(entry.getEntryNumber())
                        .referenceType(entry.getReferenceType())
                        .referenceId(entry.getReferenceId())
                        .transactionType(entry.getTransactionType())
                        .description(entry.getDescription())
                        .lineNumber(line.getLineNumber())
                        .accountCode(account != null ? account.getAccountCode() : null)
                        .accountName(account != null ? account.getAccountName() : null)
                        .lineDescription(line.getDescription())
                        .debitAmount(debit)
                        .creditAmount(credit)
                        .status(entry.getStatus())
                        .build());
            }
        }

        return new DayBookReportResponse(
                reportDate,
                (int) entryPage.getTotalElements(),
                totalDebits,
                totalCredits,
                totalDebits.subtract(totalCredits),
                dayBookEntries,
                PageMetadata.from(entryPage)
        );
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
