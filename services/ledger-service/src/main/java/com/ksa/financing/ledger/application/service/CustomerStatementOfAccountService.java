package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.CustomerStatementOfAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Customer Statement of Account by listing all journal entries
 * on customer-linked loan accounts within the date window, computing a running balance.
 *
 * Current implementation returns a scaffolded stub aligned with the existing report pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerStatementOfAccountService {

    public CustomerStatementOfAccountResponse generate(UUID tenantId,
                                                       UUID customerId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate) {
        log.debug("Generating customer statement of account tenant={} customerId={} from={} to={}",
                tenantId, customerId, fromDate, toDate);

        return CustomerStatementOfAccountResponse.builder()
                .customerId(customerId)
                .customerName(null)
                .nationalId(null)
                .fromDate(fromDate)
                .toDate(toDate)
                .openingBalance(BigDecimal.ZERO)
                .closingBalance(BigDecimal.ZERO)
                .totalDebits(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO)
                .entries(List.of())
                .build();
    }
}
