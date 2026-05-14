package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.CollectionsDueReportResponse;
import com.ksa.financing.ledger.application.service.util.SearchFilterUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CollectionsDueReportService {

    public CollectionsDueReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate, PageQuery pageQuery) {
        List<CollectionsDueReportResponse.CollectionsDueItem> allItems = List.of(
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
        );

        String search = pageQuery != null ? pageQuery.search() : null;
        List<CollectionsDueReportResponse.CollectionsDueItem> filtered = allItems.stream()
                .filter(it -> SearchFilterUtil.matchesSearch(search,
                        it.loanId() != null ? it.loanId().toString() : null,
                        it.customerId() != null ? it.customerId().toString() : null))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<CollectionsDueReportResponse.CollectionsDueItem> pagedItems = filtered.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new CollectionsDueReportResponse(
                fromDate,
                toDate,
                BigDecimal.valueOf(68500),
                pagedItems,
                new PageMetadata(page, size, totalElements, totalPages, page == 0, toIndex == totalElements, pagedItems.isEmpty())
        );
    }
}
