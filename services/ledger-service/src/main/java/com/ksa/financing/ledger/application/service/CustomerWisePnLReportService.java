package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CustomerWisePnLReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerWisePnLReportService {

    public CustomerWisePnLReportResponse generate(UUID tenantId, String period) {
        return CustomerWisePnLReportResponse.builder()
                .period(period)
                .items(List.of(
                        CustomerWisePnLReportResponse.CustomerWisePnLItem.builder()
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-pnl-customer-1").getBytes(StandardCharsets.UTF_8)))
                                .customerName("Customer A")
                                .revenue(BigDecimal.valueOf(42000))
                                .expense(BigDecimal.valueOf(2000))
                                .netProfit(BigDecimal.valueOf(40000))
                                .build(),
                        CustomerWisePnLReportResponse.CustomerWisePnLItem.builder()
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-pnl-customer-2").getBytes(StandardCharsets.UTF_8)))
                                .customerName("Customer B")
                                .revenue(BigDecimal.valueOf(35000))
                                .expense(BigDecimal.valueOf(1500))
                                .netProfit(BigDecimal.valueOf(33500))
                                .build()
                ))
                .build();
    }
}
