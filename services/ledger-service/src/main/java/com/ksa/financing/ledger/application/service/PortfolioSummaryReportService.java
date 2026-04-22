package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.PortfolioSummaryReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSummaryReportService {
    public PortfolioSummaryReportResponse generate(UUID tenantId, LocalDate from, LocalDate to) {
        return PortfolioSummaryReportResponse.builder()
            .fromDate(from).toDate(to)
            .totalDisbursed(BigDecimal.valueOf(500000))
            .outstandingBalance(BigDecimal.valueOf(450000))
            .totalCollections(BigDecimal.valueOf(50000))
            .delinquencyRate(BigDecimal.valueOf(5.5))
            .build();
    }
}
