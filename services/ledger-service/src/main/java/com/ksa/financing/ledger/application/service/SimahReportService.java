package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.SimahReportResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SimahReportService {

    public SimahReportResponse generate(UUID tenantId, String period) {
        return SimahReportResponse.builder()
                .period(period)
                .generatedDate(LocalDate.now())
                .items(List.of(
                        SimahReportResponse.SimahReportItem.builder()
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-simah-customer-1").getBytes(StandardCharsets.UTF_8)))
                                .loanId(UUID.nameUUIDFromBytes((tenantId + "-simah-loan-1").getBytes(StandardCharsets.UTF_8)))
                                .simahStatus("REPORTED")
                                .facilityType("MURABAHA")
                                .paymentStatus("CURRENT")
                                .build()
                ))
                .build();
    }
}
