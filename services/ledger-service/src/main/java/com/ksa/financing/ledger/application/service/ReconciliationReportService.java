package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.ReconciliationReportDetailResponse;
import com.ksa.financing.ledger.domain.port.out.FineractSyncPort;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaFineractAccountMappingRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Compares our internal GL balances with Fineract's GL balances for reconciliation.
 * For each active Fineract-mapped account:
 *   - Compute our closing balance from journal_lines
 *   - Fetch Fineract's running balance
 *   - Flag discrepancy if they differ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationReportService {

    private final JpaJournalEntryRepository journalEntryRepository;
    private final JpaFineractAccountMappingRepository mappingRepository;
    private final FineractSyncPort fineractSyncPort;

    @Transactional(readOnly = true)
    public ReconciliationReportDetailResponse generate(UUID tenantId, LocalDate date) {
        log.info("Generating reconciliation report: tenantId={} date={}", tenantId, date);

        var mappings = mappingRepository.findByTenantIdAndIsActiveTrue(tenantId);
        int ourEntryCount = 0;
        int fineractEntryCount = 0;
        int matching = 0;
        int discrepancies = 0;

        if (mappings.isEmpty()) {
            log.warn("No active Fineract account mappings found for tenant={}", tenantId);
            return ReconciliationReportDetailResponse.builder()
                    .reportDate(date)
                    .ourGlCount(0)
                    .fineractGlCount(0)
                    .matchingEntries(0)
                    .discrepancies(0)
                    .build();
        }

        // Our GL activity snapshot up to the report date.
        var ourEntries = journalEntryRepository.findAllByTenantAndDateRange(
                tenantId, LocalDate.of(1970, 1, 1), date);
        ourEntryCount = ourEntries.size();

        // Balance aggregation per internal account from our lines.
        Map<UUID, BigDecimal> ourBalances = new HashMap<>();
        for (JournalEntryJpaEntity entry : ourEntries) {
            if (!"POSTED".equalsIgnoreCase(entry.getStatus())) continue;
            for (JournalLineJpaEntity line : entry.getLines()) {
                BigDecimal debit = line.getDebitAmount() == null ? BigDecimal.ZERO : line.getDebitAmount();
                BigDecimal credit = line.getCreditAmount() == null ? BigDecimal.ZERO : line.getCreditAmount();
                ourBalances.merge(line.getAccountId(), debit.subtract(credit), BigDecimal::add);
            }
        }

        // Compare per mapped account.
        for (var mapping : mappings) {
            BigDecimal ourBalance = ourBalances.getOrDefault(mapping.getInternalAccountId(), BigDecimal.ZERO);
            BigDecimal fineractBalance;
            try {
                fineractBalance = fineractSyncPort.getAccountBalance(mapping.getFineractGlAccountId());
            } catch (Exception e) {
                log.warn("Fineract balance fetch failed for account {}: {}",
                        mapping.getFineractGlAccountCode(), e.getMessage());
                fineractBalance = BigDecimal.ZERO;
                discrepancies++;
                continue;
            }
            fineractEntryCount++;

            if (ourBalance.compareTo(fineractBalance) == 0) {
                matching++;
            } else {
                discrepancies++;
                log.info("Reconciliation mismatch: account={} ourBalance={} fineractBalance={}",
                        mapping.getInternalAccountCode(), ourBalance, fineractBalance);
            }
        }

        return ReconciliationReportDetailResponse.builder()
                .reportDate(date)
                .ourGlCount(ourEntryCount)
                .fineractGlCount(fineractEntryCount)
                .matchingEntries(matching)
                .discrepancies(discrepancies)
                .build();
    }
}
