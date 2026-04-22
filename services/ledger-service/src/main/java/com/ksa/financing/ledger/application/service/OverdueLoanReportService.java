package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.OverdueLoanReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Overdue Loan Report by scanning repayment schedule entries
 * whose due_date < as_of_date AND status != PAID, aggregated by loan.
 *
 * Current implementation returns a scaffolded stub aligned with the existing report pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueLoanReportService {

    public OverdueLoanReportResponse generate(UUID tenantId,
                                              LocalDate asOfDate,
                                              Integer minDaysPastDue,
                                              String productCode) {
        log.debug("Generating overdue loan report tenant={} asOf={} minDPD={} product={}",
                tenantId, asOfDate, minDaysPastDue, productCode);

        return OverdueLoanReportResponse.builder()
                .asOfDate(asOfDate)
                .totalCount(0)
                .totalOverdueAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }
}
