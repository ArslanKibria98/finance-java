package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.DPDBucketReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DPDBucketReportService {
    public DPDBucketReportResponse generate(UUID tenantId, LocalDate date) {
        return DPDBucketReportResponse.builder()
            .reportDate(date)
            .buckets(java.util.List.of())
            .build();
    }
}
