package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.LoanHistoryReportResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LoanHistoryReportService {

    public LoanHistoryReportResponse generate(UUID tenantId, UUID loanId) {
        LocalDateTime now = LocalDateTime.now();
        return LoanHistoryReportResponse.builder()
                .loanId(loanId)
                .events(List.of(
                        LoanHistoryReportResponse.LoanHistoryItem.builder()
                                .eventTime(now.minusDays(12))
                                .eventType("APPLICATION_SUBMITTED")
                                .description("Loan application submitted")
                                .build(),
                        LoanHistoryReportResponse.LoanHistoryItem.builder()
                                .eventTime(now.minusDays(9))
                                .eventType("APPROVED")
                                .description("Loan approved by underwriter")
                                .build(),
                        LoanHistoryReportResponse.LoanHistoryItem.builder()
                                .eventTime(now.minusDays(7))
                                .eventType("DISBURSED")
                                .description("Loan disbursed successfully")
                                .build()
                ))
                .build();
    }
}
