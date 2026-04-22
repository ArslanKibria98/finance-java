package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.AccountReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AccountReportService {

    public AccountReportResponse generate(UUID tenantId, LocalDate asOfDate) {
        return AccountReportResponse.builder()
                .asOfDate(asOfDate)
                .items(List.of(
                        AccountReportResponse.AccountReportItem.builder()
                                .accountCode("120101")
                                .accountName("Loans Receivable")
                                .openingBalance(BigDecimal.valueOf(800000))
                                .debits(BigDecimal.valueOf(50000))
                                .credits(BigDecimal.valueOf(35000))
                                .closingBalance(BigDecimal.valueOf(815000))
                                .build(),
                        AccountReportResponse.AccountReportItem.builder()
                                .accountCode("410201")
                                .accountName("Murabaha Profit Income")
                                .openingBalance(BigDecimal.valueOf(130000))
                                .debits(BigDecimal.ZERO)
                                .credits(BigDecimal.valueOf(22000))
                                .closingBalance(BigDecimal.valueOf(152000))
                                .build()
                ))
                .build();
    }
}
