package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.EarlySettlementReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EarlySettlementReportService {

    public EarlySettlementReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        return EarlySettlementReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .items(List.of(
                        EarlySettlementReportResponse.EarlySettlementItem.builder()
                                .loanId(UUID.nameUUIDFromBytes((tenantId + "-early-loan-1").getBytes(StandardCharsets.UTF_8)))
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-early-customer-1").getBytes(StandardCharsets.UTF_8)))
                                .settlementDate(fromDate.plusDays(4))
                                .outstandingAmount(BigDecimal.valueOf(42000))
                                .rebateAmount(BigDecimal.valueOf(1800))
                                .build()
                ))
                .build();
    }
}
