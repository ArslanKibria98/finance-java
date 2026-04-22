package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.ProductWisePnLReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductWisePnLReportService {

    public ProductWisePnLReportResponse generate(UUID tenantId, String period) {
        return ProductWisePnLReportResponse.builder()
                .period(period)
                .items(List.of(
                        ProductWisePnLReportResponse.ProductWisePnLItem.builder()
                                .productCode("MURABAHA")
                                .productName("Murabaha")
                                .revenue(BigDecimal.valueOf(120000))
                                .expense(BigDecimal.valueOf(15000))
                                .netProfit(BigDecimal.valueOf(105000))
                                .build(),
                        ProductWisePnLReportResponse.ProductWisePnLItem.builder()
                                .productCode("IJARA")
                                .productName("Ijara")
                                .revenue(BigDecimal.valueOf(82000))
                                .expense(BigDecimal.valueOf(10000))
                                .netProfit(BigDecimal.valueOf(72000))
                                .build()
                ))
                .build();
    }
}
