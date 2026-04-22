package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.TrialBalanceAccountResponse;
import com.ksa.financing.ledger.application.dto.TrialBalanceReportResponse;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrialBalanceReportService {

    private final JpaAccountRepository accountRepository;
    private final JpaAccountBalanceRepository accountBalanceRepository;

    public TrialBalanceReportResponse generate(UUID tenantId, LocalDate date) {
        log.info("Generating trial balance report: tenant={}, date={}", tenantId, date);

        var balances = accountBalanceRepository.findAllByTenantIdAndBalanceDate(tenantId, date);

        var accountResponses = new ArrayList<TrialBalanceAccountResponse>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (var balance : balances) {
            var account = accountRepository.findByTenantIdAndId(tenantId, balance.getAccountId()).orElse(null);
            if (account != null) {
                BigDecimal closingBalance = balance.getClosingBalance() != null ? balance.getClosingBalance() : BigDecimal.ZERO;
                String accountType = account.getAccountType() != null ? account.getAccountType().toString() : "UNKNOWN";
                
                if ("ASSET".equals(accountType) || "EXPENSE".equals(accountType)) {
                    totalDebits = totalDebits.add(closingBalance);
                } else {
                    totalCredits = totalCredits.add(closingBalance);
                }

                accountResponses.add(TrialBalanceAccountResponse.builder()
                    .accountCode(account.getAccountCode())
                    .accountName(account.getAccountName())
                    .accountType(accountType)
                    .debitBalance(isDebitType(accountType) ? closingBalance : BigDecimal.ZERO)
                    .creditBalance(isDebitType(accountType) ? BigDecimal.ZERO : closingBalance)
                    .build());
            }
        }

        return TrialBalanceReportResponse.builder()
            .reportDate(date)
            .accounts(accountResponses)
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .difference(totalDebits.subtract(totalCredits))
            .build();
    }

    private boolean isDebitType(String accountType) {
        return "ASSET".equals(accountType) || "EXPENSE".equals(accountType);
    }
}
