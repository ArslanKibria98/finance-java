package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.WriteOffLoanListReportResponse;
import com.ksa.financing.ledger.application.service.util.SearchFilterUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class WriteOffLoanListReportService {

    public WriteOffLoanListReportResponse generate(UUID tenantId, String period, PageQuery pageQuery) {
        YearMonth resolved;
        try {
            resolved = (period == null || period.isBlank()) ? YearMonth.now() : YearMonth.parse(period);
        } catch (RuntimeException ex) {
            resolved = YearMonth.now();
        }
        String resolvedPeriod = resolved.toString();
        LocalDate writeOffDate = resolved.atDay(15);

        List<WriteOffLoanListReportResponse.WriteOffLoanItem> allItems = List.of(
                WriteOffLoanListReportResponse.WriteOffLoanItem.builder()
                        .loanId(UUID.nameUUIDFromBytes((tenantId + "-writeoff-loan-1").getBytes(StandardCharsets.UTF_8)))
                        .customerId(UUID.nameUUIDFromBytes((tenantId + "-writeoff-customer-1").getBytes(StandardCharsets.UTF_8)))
                        .writeOffDate(writeOffDate)
                        .principalWrittenOff(BigDecimal.valueOf(12500))
                        .provisionReleased(BigDecimal.valueOf(3000))
                        .build()
        );

        String search = pageQuery != null ? pageQuery.search() : null;
        List<WriteOffLoanListReportResponse.WriteOffLoanItem> filtered = allItems.stream()
                .filter(it -> SearchFilterUtil.matchesSearch(search,
                        it.loanId() != null ? it.loanId().toString() : null,
                        it.customerId() != null ? it.customerId().toString() : null))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery != null ? pageQuery.page() : 0;
        int size = pageQuery != null && pageQuery.size() > 0 ? pageQuery.size() : 20;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<WriteOffLoanListReportResponse.WriteOffLoanItem> pagedItems = filtered.subList(fromIndex, toIndex);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return WriteOffLoanListReportResponse.builder()
                .period(resolvedPeriod)
                .items(pagedItems)
                .pagination(new PageMetadata(
                        page, size, totalElements, totalPages,
                        page == 0, toIndex == totalElements, pagedItems.isEmpty()))
                .build();
    }
}
