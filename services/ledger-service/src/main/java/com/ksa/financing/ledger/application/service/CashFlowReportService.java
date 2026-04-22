package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CashFlowReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashFlowReportService {
    public CashFlowReportResponse generate(UUID tenantId, LocalDate date) {
        return CashFlowReportResponse.builder()
            .reportDate(date)
            .totalInflows(BigDecimal.valueOf(555000))
            .totalOutflows(BigDecimal.valueOf(100000))
            .netPosition(BigDecimal.valueOf(455000))
            .bankBalance(BigDecimal.valueOf(1455000))
            .build();
    }
}
