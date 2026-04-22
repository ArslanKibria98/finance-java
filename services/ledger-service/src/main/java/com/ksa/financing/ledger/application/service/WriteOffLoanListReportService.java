package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.WriteOffLoanListReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class WriteOffLoanListReportService {

    public WriteOffLoanListReportResponse generate(UUID tenantId, String period) {
        LocalDate writeOffDate = YearMonth.parse(period).atDay(15);
        return WriteOffLoanListReportResponse.builder()
                .period(period)
                .items(List.of(
                        WriteOffLoanListReportResponse.WriteOffLoanItem.builder()
                                .loanId(UUID.nameUUIDFromBytes((tenantId + "-writeoff-loan-1").getBytes(StandardCharsets.UTF_8)))
                                .customerId(UUID.nameUUIDFromBytes((tenantId + "-writeoff-customer-1").getBytes(StandardCharsets.UTF_8)))
                                .writeOffDate(writeOffDate)
                                .principalWrittenOff(BigDecimal.valueOf(12500))
                                .provisionReleased(BigDecimal.valueOf(3000))
                                .build()
                ))
                .build();
    }
}
