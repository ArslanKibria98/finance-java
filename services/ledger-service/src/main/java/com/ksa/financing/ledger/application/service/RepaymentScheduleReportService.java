package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.RepaymentScheduleReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Repayment Schedule Report for a single loan by reading its
 * installment plan (principal, profit, penalty, paid amount, status per installment).
 *
 * Current implementation returns a scaffolded stub aligned with the existing report pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentScheduleReportService {

    public RepaymentScheduleReportResponse generate(UUID tenantId, UUID loanId) {
        log.debug("Generating repayment schedule report tenant={} loanId={}", tenantId, loanId);

        return RepaymentScheduleReportResponse.builder()
                .loanId(loanId)
                .loanAccountNumber(null)
                .customerName(null)
                .productName(null)
                .disbursedPrincipal(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                .totalPayable(BigDecimal.ZERO)
                .tenureMonths(0)
                .installments(List.of())
                .build();
    }

    public List<RepaymentScheduleReportResponse> generateAll(
            UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        log.debug("Generating repayment schedule list tenant={} fromDate={} toDate={}", tenantId, fromDate, toDate);
        return List.of();
    }
}
