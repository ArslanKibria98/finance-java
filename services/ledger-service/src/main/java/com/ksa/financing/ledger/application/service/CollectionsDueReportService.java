package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CollectionsDueReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CollectionsDueReportService {

    public CollectionsDueReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        return CollectionsDueReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalDue(BigDecimal.valueOf(68500))
                .items(List.of(
                        CollectionsDueReportResponse.CollectionsDueItem.builder()
                                .loanId(UUID.nameUUIDFromBytes((tenantId + "-due-loan-1").getBytes(StandardCharsets.UTF_8)))
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-due-customer-1").getBytes(StandardCharsets.UTF_8)))
                                .dueDate(fromDate.plusDays(2))
                                .dueAmount(BigDecimal.valueOf(8500))
                                .build(),
                        CollectionsDueReportResponse.CollectionsDueItem.builder()
                                .loanId(UUID.nameUUIDFromBytes((tenantId + "-due-loan-2").getBytes(StandardCharsets.UTF_8)))
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-due-customer-2").getBytes(StandardCharsets.UTF_8)))
                                .dueDate(fromDate.plusDays(5))
                                .dueAmount(BigDecimal.valueOf(12000))
                                .build()
                ))
                .build();
    }
}
