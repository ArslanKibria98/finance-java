package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.LoanHistoryReportResponse;
import com.ksa.financing.ledger.application.service.util.SearchFilterUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LoanHistoryReportService {

    public LoanHistoryReportResponse generate(UUID tenantId, UUID loanId, PageQuery pageQuery) {
        LocalDateTime now = LocalDateTime.now();
        List<LoanHistoryReportResponse.LoanHistoryItem> allItems = List.of(
                LoanHistoryReportResponse.LoanHistoryItem.builder()
                        .eventTime(now.minusDays(12))
                        .eventType("APPLICATION_SUBMITTED")
                        .description("Loan application submitted")
                        .build(),
                LoanHistoryReportResponse.LoanHistoryItem.builder()
                        .eventTime(now.minusDays(9))
                        .eventType("APPROVED")
                        .description("Loan approved by underwriter")
                        .build(),
                LoanHistoryReportResponse.LoanHistoryItem.builder()
                        .eventTime(now.minusDays(7))
                        .eventType("DISBURSED")
                        .description("Loan disbursed successfully")
                        .build()
        );

        String search = pageQuery != null ? pageQuery.search() : null;
        List<LoanHistoryReportResponse.LoanHistoryItem> filtered = allItems.stream()
                .filter(it -> SearchFilterUtil.matchesSearch(search,
                        it.eventType(), it.description()))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<LoanHistoryReportResponse.LoanHistoryItem> pagedItems = filtered.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new LoanHistoryReportResponse(
                loanId,
                pagedItems,
                new PageMetadata(page, size, totalElements, totalPages, page == 0, toIndex == totalElements, pagedItems.isEmpty())
        );
    }
}
