package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.NplReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class NplReportService {

    public NplReportResponse generate(UUID tenantId, LocalDate asOfDate) {
        return NplReportResponse.builder()
                .asOfDate(asOfDate)
                .totalOutstanding(BigDecimal.valueOf(950000))
                .nplOutstanding(BigDecimal.valueOf(118000))
                .nplRatio(BigDecimal.valueOf(12.42))
                .buckets(List.of(
                        NplReportResponse.NplBucket.builder()
                                .bucket("90-179")
                                .outstanding(BigDecimal.valueOf(67000))
                                .build(),
                        NplReportResponse.NplBucket.builder()
                                .bucket("180+")
                                .outstanding(BigDecimal.valueOf(51000))
                                .build()
                ))
                .build();
    }
}
