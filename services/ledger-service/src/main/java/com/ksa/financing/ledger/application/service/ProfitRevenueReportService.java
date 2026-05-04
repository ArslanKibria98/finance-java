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
        if (values == null || values.length <= index || values[index] == null) {
            // Some PostgreSQL drivers may return a single composite tuple "(debit,credit)".
            if (values != null && values.length == 1 && values[0] != null) {
                var composite = parseCompositeTuple(values[0].toString(), index);
                if (composite != null) {
                    return composite;
                }
            }
            return BigDecimal.ZERO;
        }
        var raw = values[index];
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        var asString = raw.toString().trim();
        if (asString.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(asString);
        } catch (NumberFormatException ex) {
            // Guard against driver/vendor-specific tuple/scalar formatting.
            var composite = parseCompositeTuple(asString, index);
            if (composite != null) {
                return composite;
            }
            log.warn("Unable to parse numeric aggregate value '{}' at index {}. Falling back to 0.", asString, index);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseCompositeTuple(String value, int index) {
        var text = value == null ? "" : value.trim();
        if (!text.startsWith("(") || !text.endsWith(")")) {
            return null;
        }
        var body = text.substring(1, text.length() - 1);
        var parts = body.split(",");
        if (parts.length <= index) {
            return BigDecimal.ZERO;
        }
        var token = parts[index].trim();
        if (token.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(token);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate[] resolvePeriod(String period) {
        try {
            var ym = YearMonth.parse(period);
            return new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
        } catch (DateTimeParseException | NullPointerException e) {
            var ym = YearMonth.now();
            return new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
        }
    }
}
