package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CashFlowReportResponse;
import com.ksa.financing.ledger.application.service.util.AggregateValueUtil;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.infrastructure.messaging.LedgerAccountCodes;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountBalanceRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashFlowReportService {

    private final AccountRepository accountRepository;
    private final JpaJournalLineRepository journalLineRepository;
    private final JpaAccountBalanceRepository accountBalanceRepository;

    @Transactional(readOnly = true)
    public CashFlowReportResponse generate(UUID tenantId, LocalDate date) {
        log.info("Generating cash flow report: tenantId={} date={}", tenantId, date);

        var bankAccount = accountRepository.findByCode(tenantId, LedgerAccountCodes.BANK_ACCOUNT)
                .orElse(null);

        if (bankAccount == null) {
            log.warn("Bank account (1010) not found for tenant={}", tenantId);
            return CashFlowReportResponse.builder()
                    .reportDate(date)
                    .totalInflows(BigDecimal.ZERO)
                    .totalOutflows(BigDecimal.ZERO)
                    .netPosition(BigDecimal.ZERO)
                    .bankBalance(BigDecimal.ZERO)
                    .build();
        }

        var bankAccountUuid = bankAccount.getId().value();
        var from = date.withDayOfMonth(1);

        // Bank is an ASSET — debits increase cash (inflows), credits decrease (outflows).
        Object[] totals;
        try {
            totals = journalLineRepository.sumDebitsCreditsByAccountInRange(
                    tenantId, bankAccountUuid, from, date);
        } catch (RuntimeException ex) {
            log.warn("Failed to aggregate cash flow totals tenant={} date={}. Returning zeros. Cause={}",
                    tenantId, date, ex.getMessage());
            totals = null;
        }
        var inflows = AggregateValueUtil.valueAt(totals, 0);
        var outflows = AggregateValueUtil.valueAt(totals, 1);
        var net = inflows.subtract(outflows);

        // Use latest balance snapshot ≤ date if available; else compute from activity.
        var bankBalance = accountBalanceRepository
                .findMostRecentBalanceBefore(tenantId, bankAccountUuid, date.plusDays(1))
                .map(b -> b.getClosingBalance())
                .orElse(net);

        return CashFlowReportResponse.builder()
                .reportDate(date)
                .totalInflows(inflows)
                .totalOutflows(outflows)
                .netPosition(net)
                .bankBalance(bankBalance)
                .build();
    }

}
