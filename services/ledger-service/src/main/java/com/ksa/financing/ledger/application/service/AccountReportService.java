package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.AccountReportResponse;
import com.ksa.financing.ledger.application.service.util.SearchFilterUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AccountReportService {

    public AccountReportResponse generate(UUID tenantId, LocalDate asOfDate, PageQuery pageQuery) {
        List<AccountReportResponse.AccountReportItem> allItems = List.of(
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
        );

        String search = pageQuery != null ? pageQuery.search() : null;
        List<AccountReportResponse.AccountReportItem> filtered = allItems.stream()
                .filter(it -> SearchFilterUtil.matchesSearch(search,
                        it.accountCode(), it.accountName()))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AccountReportResponse.AccountReportItem> pagedItems = filtered.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new AccountReportResponse(
                asOfDate,
                pagedItems,
                new PageMetadata(page, size, totalElements, totalPages, page == 0, toIndex == totalElements, pagedItems.isEmpty())
        );
    }
}
