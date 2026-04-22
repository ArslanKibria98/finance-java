package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.LoanBalanceOutstandingReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Loan Balance & Outstanding Report by computing, per loan:
 *   disbursed - paid = principal_outstanding
 *   accrued_profit - profit_collected = profit_outstanding
 *   accrued_penalty - penalty_collected = penalties_outstanding
 *
 * Current implementation returns a scaffolded stub aligned with the existing report pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanBalanceOutstandingReportService {

    public LoanBalanceOutstandingReportResponse generate(UUID tenantId,
                                                         LocalDate asOfDate,
                                                         UUID customerId,
                                                         String productCode) {
        log.debug("Generating loan balance & outstanding report tenant={} asOf={} customer={} product={}",
                tenantId, asOfDate, customerId, productCode);

        return LoanBalanceOutstandingReportResponse.builder()
                .asOfDate(asOfDate)
                .totalCount(0)
                .totalPrincipalOutstanding(BigDecimal.ZERO)
                .totalProfitOutstanding(BigDecimal.ZERO)
                .totalPenaltiesOutstanding(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }
}
