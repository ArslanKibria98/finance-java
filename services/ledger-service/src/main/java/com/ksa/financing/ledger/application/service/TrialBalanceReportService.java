package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.TrialBalanceAccountResponse;
import com.ksa.financing.ledger.application.dto.TrialBalanceReportResponse;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountBalanceRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrialBalanceReportService {

    private final JpaAccountRepository accountRepository;
    private final JpaAccountBalanceRepository accountBalanceRepository;

    public TrialBalanceReportResponse generate(UUID tenantId, LocalDate date, PageQuery pageQuery) {
        log.info("Generating trial balance report: tenant={}, date={}", tenantId, date);

        var balances = (date == null)
                ? accountBalanceRepository.findLatestBalancesPerAccount(tenantId)
                : accountBalanceRepository.findAllByTenantIdAndBalanceDate(tenantId, date);

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

        // Apply optional search filter (case-insensitive across code/name/type)
        var filtered = applySearch(accountResponses, pageQuery != null ? pageQuery.search() : null);

        // Slice for pagination
        int totalElements = filtered.size();
        int page = pageQuery != null ? pageQuery.page() : 0;
        int size = pageQuery != null && pageQuery.size() > 0 ? pageQuery.size() : Math.max(totalElements, 1);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        var pagedItems = filtered.subList(fromIndex, toIndex);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return TrialBalanceReportResponse.builder()
            .reportDate(date)
            .accounts(pagedItems)
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .difference(totalDebits.subtract(totalCredits))
            .pagination(new PageMetadata(
                    page, size, totalElements, totalPages,
                    page == 0, toIndex == totalElements, pagedItems.isEmpty()))
            .build();
    }

    private java.util.List<TrialBalanceAccountResponse> applySearch(
            java.util.List<TrialBalanceAccountResponse> items, String search) {
        if (search == null || search.isBlank()) {
            return items;
        }
        String needle = search.toLowerCase();
        var out = new ArrayList<TrialBalanceAccountResponse>();
        for (var it : items) {
            if (matches(it.accountCode(), needle)
                    || matches(it.accountName(), needle)
                    || matches(it.accountType(), needle)) {
                out.add(it);
            }
        }
        return out;
    }

    private boolean matches(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private boolean isDebitType(String accountType) {
        return "ASSET".equals(accountType) || "EXPENSE".equals(accountType);
    }
}
