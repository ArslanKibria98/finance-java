package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.DailyTransactionSummaryReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DailyTransactionSummaryReportService {

    public DailyTransactionSummaryReportResponse generate(UUID tenantId, LocalDate date) {
        return DailyTransactionSummaryReportResponse.builder()
                .date(date)
                .totalCredits(BigDecimal.valueOf(240000))
                .totalDebits(BigDecimal.valueOf(210000))
                .transactionCount(148)
                .channels(List.of(
                        DailyTransactionSummaryReportResponse.ChannelSummary.builder()
                                .channel("BANK_TRANSFER")
                                .amount(BigDecimal.valueOf(125000))
                                .count(53)
                                .build(),
                        DailyTransactionSummaryReportResponse.ChannelSummary.builder()
                                .channel("WALLET")
                                .amount(BigDecimal.valueOf(115000))
                                .count(95)
                                .build()
                ))
                .build();
    }
}
