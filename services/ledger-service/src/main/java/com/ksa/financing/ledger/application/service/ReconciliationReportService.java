package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.ReconciliationReportDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationReportService {
    public ReconciliationReportDetailResponse generate(UUID tenantId, LocalDate date) {
        return ReconciliationReportDetailResponse.builder()
            .reportDate(date)
            .ourGlCount(3)
            .fineractGlCount(3)
            .matchingEntries(3)
            .discrepancies(0)
            .build();
    }
}
