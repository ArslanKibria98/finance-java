package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.ProfitRevenueReportResponse;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.infrastructure.messaging.LedgerAccountCodes;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitRevenueReportService {

    private final AccountRepository accountRepository;
    private final JpaJournalLineRepository journalLineRepository;

    @Transactional(readOnly = true)
    public ProfitRevenueReportResponse generate(UUID tenantId, String period) {
        log.info("Generating profit revenue report: tenantId={} period={}", tenantId, period);
        try {
            var range = resolvePeriod(period);
            var from = range[0];
            var to = range[1];

            var profitAccount = accountRepository.findByCode(tenantId, LedgerAccountCodes.PROFIT_INCOME)
                    .orElse(null);

            if (profitAccount == null) {
                log.warn("Profit income account (4010) not found for tenant={}", tenantId);
                return emptyResponse(period);
            }

            var profitUuid = profitAccount.getId().value();

            // Income account: credits increase profit (earned), debits reverse it (ibra).
            Object[] totals;
            try {
                totals = journalLineRepository.sumDebitsCreditsByAccountInRange(
                        tenantId, profitUuid, from, to);
            } catch (RuntimeException ex) {
                log.warn("Failed to aggregate profit totals for tenant={} period={}. Returning zeros. Cause={}",
                        tenantId, period, ex.getMessage());
                return emptyResponse(period);
            }

            var totalDebits = valueAt(totals, 0);
            var totalCredits = valueAt(totals, 1);

            BigDecimal profitEarned = totalCredits.subtract(totalDebits);
            if (profitEarned.compareTo(BigDecimal.ZERO) < 0) {
                profitEarned = BigDecimal.ZERO;
            }

            // Collected = earned for cash-basis MVP (proper deferred accrual model is future work).
            BigDecimal profitCollected = profitEarned;
            BigDecimal accrued = BigDecimal.ZERO;

            return ProfitRevenueReportResponse.builder()
                    .period(period)
                    .profitEarned(profitEarned)
                    .profitCollected(profitCollected)
                    .accruedProfit(accrued)
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Profit revenue report fallback triggered for tenant={} period={}. Cause={}",
                    tenantId, period, ex.getMessage());
            return emptyResponse(period);
        }
    }

    private ProfitRevenueReportResponse emptyResponse(String period) {
        return ProfitRevenueReportResponse.builder()
                .period(period)
                .profitEarned(BigDecimal.ZERO)
                .profitCollected(BigDecimal.ZERO)
                .accruedProfit(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal valueAt(Object[] values, int index) {
        return com.ksa.financing.ledger.application.service.util.AggregateValueUtil.valueAt(values, index);
    }

    private LocalDate[] resolvePeriod(String period) {
        if (period == null || period.isBlank()) {
            return new LocalDate[] { LocalDate.of(1900, 1, 1), LocalDate.now() };
        }
        try {
            var ym = YearMonth.parse(period);
            return new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
        } catch (DateTimeParseException e) {
            var ym = YearMonth.now();
            return new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
        }
    }
}
