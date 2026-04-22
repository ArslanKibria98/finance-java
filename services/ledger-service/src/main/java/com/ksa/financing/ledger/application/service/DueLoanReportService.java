package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.DueLoanReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Due Loan Report by selecting repayment schedule rows with
 * due_date BETWEEN from AND to AND status IN (PENDING, PARTIALLY_PAID).
 *
 * Current implementation returns a scaffolded stub aligned with the existing report pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DueLoanReportService {

    public DueLoanReportResponse generate(UUID tenantId,
                                          LocalDate fromDate,
                                          LocalDate toDate) {
        log.debug("Generating due loan report tenant={} from={} to={}", tenantId, fromDate, toDate);

        return DueLoanReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalCount(0)
                .totalDueAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }
}
