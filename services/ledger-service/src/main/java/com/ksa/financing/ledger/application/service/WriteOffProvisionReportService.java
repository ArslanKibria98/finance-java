package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.WriteOffProvisionReportResponse;
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
public class WriteOffProvisionReportService {

    private final AccountRepository accountRepository;
    private final JpaJournalLineRepository journalLineRepository;

    @Transactional(readOnly = true)
    public WriteOffProvisionReportResponse generate(UUID tenantId, String period) {
        log.info("Generating write-off/provision report: tenantId={} period={}", tenantId, period);

        var range = resolvePeriod(period);
        var from = range[0];
        var to = range[1];

        var provisionAccount = accountRepository.findByCode(tenantId, LedgerAccountCodes.PROVISION_FOR_BAD_DEBTS)
                .orElse(null);

        if (provisionAccount == null) {
            log.warn("Provision account (3100) not found for tenant={}", tenantId);
            return WriteOffProvisionReportResponse.builder()
                    .period(period)
                    .totalWriteOffs(BigDecimal.ZERO)
                    .badDebtProvisions(BigDecimal.ZERO)
                    .restructuredLoans(BigDecimal.ZERO)
                    .build();
        }

        var provisionUuid = provisionAccount.getId().value();

        // Expense account: debits increase provisions. Credits reverse them (write-offs from reserve).
        var totals = journalLineRepository.sumDebitsCreditsByAccountInRange(
                tenantId, provisionUuid, from, to);
        BigDecimal provisionsRaised = toBigDecimal(totals, 0);
        BigDecimal writeOffs = toBigDecimal(totals, 1);

        return WriteOffProvisionReportResponse.builder()
                .period(period)
                .totalWriteOffs(writeOffs)
                .badDebtProvisions(provisionsRaised)
                .restructuredLoans(BigDecimal.ZERO)
                .build();
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

    private BigDecimal toBigDecimal(Object[] row, int idx) {
        if (row == null || row.length <= idx || row[idx] == null) return BigDecimal.ZERO;
        return new BigDecimal(row[idx].toString());
    }
}
