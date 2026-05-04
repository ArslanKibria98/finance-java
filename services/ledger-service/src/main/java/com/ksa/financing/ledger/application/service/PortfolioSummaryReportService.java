package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.PortfolioSummaryReportResponse;
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
public class PortfolioSummaryReportService {

    private final AccountRepository accountRepository;
    private final JpaJournalLineRepository journalLineRepository;

    @Transactional(readOnly = true)
    public PortfolioSummaryReportResponse generate(UUID tenantId, LocalDate from, LocalDate to) {
        log.info("Generating portfolio summary: tenantId={} from={} to={}", tenantId, from, to);

        var receivableAccount = accountRepository.findByCode(tenantId, LedgerAccountCodes.LOANS_RECEIVABLE)
                .orElse(null);

        if (receivableAccount == null) {
            log.warn("Loans receivable account (1200) not found for tenant={}", tenantId);
            return PortfolioSummaryReportResponse.builder()
                    .fromDate(from).toDate(to)
                    .totalDisbursed(BigDecimal.ZERO)
                    .outstandingBalance(BigDecimal.ZERO)
                    .totalCollections(BigDecimal.ZERO)
                    .delinquencyRate(BigDecimal.ZERO)
                    .build();
        }

        var receivableUuid = receivableAccount.getId().value();

        // In-range totals: debits = disbursements, credits = collections.
        var rangeTotals = journalLineRepository.sumDebitsCreditsByAccountInRange(
                tenantId, receivableUuid, from, to);
        BigDecimal disbursedInRange = toBigDecimal(rangeTotals, 0);
        BigDecimal collectedInRange = toBigDecimal(rangeTotals, 1);

        // Outstanding balance = cumulative debits - cumulative credits up to `to` date.
        var cumulative = journalLineRepository.sumDebitsCreditsByAccountUpToDate(
                tenantId, receivableUuid, to);
        BigDecimal outstanding = toBigDecimal(cumulative, 0).subtract(toBigDecimal(cumulative, 1));
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }

        // Delinquency rate: provisions raised / outstanding (rough MVP proxy).
        BigDecimal delinquencyRate = BigDecimal.ZERO;
        var provisionAccount = accountRepository.findByCode(tenantId, LedgerAccountCodes.PROVISION_FOR_BAD_DEBTS)
                .orElse(null);
        if (provisionAccount != null && outstanding.compareTo(BigDecimal.ZERO) > 0) {
            var provisionTotals = journalLineRepository.sumDebitsCreditsByAccountUpToDate(
                    tenantId, provisionAccount.getId().value(), to);
            BigDecimal provisions = toBigDecimal(provisionTotals, 0)
                    .subtract(toBigDecimal(provisionTotals, 1));
            if (provisions.compareTo(BigDecimal.ZERO) > 0) {
                delinquencyRate = provisions.multiply(BigDecimal.valueOf(100))
                        .divide(outstanding, 2, RoundingMode.HALF_UP);
            }
        }

        return PortfolioSummaryReportResponse.builder()
                .fromDate(from).toDate(to)
                .totalDisbursed(disbursedInRange)
                .outstandingBalance(outstanding)
                .totalCollections(collectedInRange)
                .delinquencyRate(delinquencyRate)
                .build();
    }

    private BigDecimal toBigDecimal(Object[] row, int idx) {
        if (row == null || row.length <= idx || row[idx] == null) return BigDecimal.ZERO;
        return new BigDecimal(row[idx].toString());
    }
}
