package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.ProfitRevenueReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitRevenueReportService {
    public ProfitRevenueReportResponse generate(UUID tenantId, String period) {
        return ProfitRevenueReportResponse.builder()
            .period(period)
            .profitEarned(BigDecimal.valueOf(5000))
            .profitCollected(BigDecimal.valueOf(5000))
            .accruedProfit(BigDecimal.ZERO)
            .build();
    }
}
