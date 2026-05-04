package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CollectionsReportResponse;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.infrastructure.messaging.LedgerAccountCodes;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionsReportService {

    private final AccountRepository accountRepository;
    private final JpaJournalLineRepository journalLineRepository;

    @Transactional(readOnly = true)
    public CollectionsReportResponse generate(UUID tenantId, LocalDate from, LocalDate to) {
        log.info("Generating collections report: tenantId={} from={} to={}", tenantId, from, to);

        var receivableAccount = accountRepository.findByCode(tenantId, LedgerAccountCodes.LOANS_RECEIVABLE)
                .orElse(null);

        if (receivableAccount == null) {
            log.warn("Loans receivable account (1200) not found for tenant={}", tenantId);
            return CollectionsReportResponse.builder()
                    .fromDate(from).toDate(to)
                    .totalCollected(BigDecimal.ZERO)
                    .onTimeRate(BigDecimal.ZERO)
                    .collectionCount(0)
                    .build();
        }

        var receivableUuid = receivableAccount.getId().value();

        // Repayment entries credit the receivable account.
        var breakdown = journalLineRepository.sumByTransactionTypeForAccountInRange(
                tenantId, receivableUuid, from, to);

        BigDecimal totalCollected = BigDecimal.ZERO;
        int collectionCount = 0;
        int onTimeCount = 0;

        for (Object[] row : breakdown) {
            String txnType = row[0] == null ? "" : row[0].toString();
            BigDecimal credits = new BigDecimal(row[2].toString());
            long count = ((Number) row[3]).longValue();

            if (LedgerAccountCodes.TXN_TYPE_REPAYMENT.equals(txnType)
                    || LedgerAccountCodes.TXN_TYPE_SETTLEMENT.equals(txnType)) {
                totalCollected = totalCollected.add(credits);
                collectionCount += (int) count;
                if (LedgerAccountCodes.TXN_TYPE_REPAYMENT.equals(txnType)) {
                    // Heuristic: standard repayments treated as on-time until DPD tracking is wired.
                    onTimeCount += (int) count;
                }
            }
        }

        BigDecimal onTimeRate = collectionCount > 0
                ? BigDecimal.valueOf(onTimeCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(collectionCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return CollectionsReportResponse.builder()
                .fromDate(from).toDate(to)
                .totalCollected(totalCollected)
                .onTimeRate(onTimeRate)
                .collectionCount(collectionCount)
                .build();
    }
}
