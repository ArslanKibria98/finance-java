package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.LoanDisbursementReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Loan Disbursement Report from GL disbursement journal entries
 * (debit loan principal account / credit disbursement clearing) posted during the range.
 *
 * Current implementation returns a scaffolded stub aligned with the existing report pattern;
 * wire to JournalEntryRepository queries once the GL disbursement schema is finalized.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanDisbursementReportService {

    public LoanDisbursementReportResponse generate(UUID tenantId,
                                                   LocalDate fromDate,
                                                   LocalDate toDate,
                                                   String productCode,
                                                   String branchOrChannel,
                                                   String status) {
        log.debug("Generating loan disbursement report tenant={} from={} to={} product={} branch={} status={}",
                tenantId, fromDate, toDate, productCode, branchOrChannel, status);

        return LoanDisbursementReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalCount(0)
                .totalDisbursedAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }
}
