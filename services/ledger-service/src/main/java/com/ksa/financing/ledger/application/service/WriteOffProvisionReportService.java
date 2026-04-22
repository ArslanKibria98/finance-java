package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.WriteOffProvisionReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WriteOffProvisionReportService {
    public WriteOffProvisionReportResponse generate(UUID tenantId, String period) {
        return WriteOffProvisionReportResponse.builder()
            .period(period)
            .totalWriteOffs(BigDecimal.ZERO)
            .badDebtProvisions(BigDecimal.ZERO)
            .restructuredLoans(BigDecimal.ZERO)
            .build();
    }
}
