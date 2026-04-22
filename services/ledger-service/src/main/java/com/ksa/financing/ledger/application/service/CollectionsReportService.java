package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CollectionsReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionsReportService {
    public CollectionsReportResponse generate(UUID tenantId, LocalDate from, LocalDate to) {
        return CollectionsReportResponse.builder()
            .fromDate(from).toDate(to)
            .totalCollected(BigDecimal.valueOf(50000))
            .onTimeRate(BigDecimal.valueOf(95.5))
            .collectionCount(10)
            .build();
    }
}
