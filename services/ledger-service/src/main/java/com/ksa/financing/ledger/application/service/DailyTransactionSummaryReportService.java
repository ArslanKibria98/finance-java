package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.DailyTransactionSummaryReportResponse;
import com.ksa.financing.ledger.application.dto.DailyTransactionSummaryReportResponse.ChannelSummary;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Generates the Daily Transaction Summary — totals for credits / debits and a
 * per-transaction-type breakdown ("channels") computed from posted journal
 * entries on the given report date.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DailyTransactionSummaryReportService {

    private static final String UNKNOWN_CHANNEL = "OTHER";

    private final JpaJournalEntryRepository journalEntryRepository;

    public DailyTransactionSummaryReportResponse generate(UUID tenantId, LocalDate date) {
        log.info("Generating daily transaction summary: tenant={}, date={}", tenantId, date);

        List<JournalEntryJpaEntity> entries = journalEntryRepository
                .findByTenantIdAndEntryDate(tenantId, date);

        if (entries.isEmpty()) {
            return DailyTransactionSummaryReportResponse.builder()
                    .date(date)
                    .totalCredits(BigDecimal.ZERO)
                    .totalDebits(BigDecimal.ZERO)
                    .transactionCount(0)
                    .channels(List.of())
                    .build();
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        Map<String, ChannelAccumulator> byChannel = new HashMap<>();

        for (JournalEntryJpaEntity entry : entries) {
            String channel = resolveChannel(entry);
            ChannelAccumulator acc = byChannel.computeIfAbsent(channel, k -> new ChannelAccumulator());
            acc.count++;

            for (JournalLineJpaEntity line : entry.getLines()) {
                BigDecimal debit = nullSafe(line.getDebitAmount());
                BigDecimal credit = nullSafe(line.getCreditAmount());
                totalDebits = totalDebits.add(debit);
                totalCredits = totalCredits.add(credit);
                // Channel amount = posted volume (one side of the balanced entry).
                acc.amount = acc.amount.add(debit);
            }
        }

        List<ChannelSummary> channels = new ArrayList<>();
        byChannel.forEach((name, acc) -> channels.add(ChannelSummary.builder()
                .channel(name)
                .amount(acc.amount)
                .count(acc.count)
                .build()));
        channels.sort(Comparator.comparing(ChannelSummary::amount).reversed());

        return DailyTransactionSummaryReportResponse.builder()
                .date(date)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .transactionCount(entries.size())
                .channels(channels)
                .build();
    }

    private String resolveChannel(JournalEntryJpaEntity entry) {
        if (entry.getTransactionType() != null && !entry.getTransactionType().isBlank()) {
            return entry.getTransactionType();
        }
        if (entry.getReferenceType() != null && !entry.getReferenceType().isBlank()) {
            return entry.getReferenceType();
        }
        return UNKNOWN_CHANNEL;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static final class ChannelAccumulator {
        BigDecimal amount = BigDecimal.ZERO;
        int count = 0;
    }
}
