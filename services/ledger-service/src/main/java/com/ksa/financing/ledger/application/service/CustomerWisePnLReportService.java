package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.CustomerWisePnLReportResponse;
import com.ksa.financing.ledger.application.service.util.SearchFilterUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerWisePnLReportService {

    public CustomerWisePnLReportResponse generate(UUID tenantId, String period, PageQuery pageQuery) {
        List<CustomerWisePnLReportResponse.CustomerWisePnLItem> allItems = List.of(
                CustomerWisePnLReportResponse.CustomerWisePnLItem.builder()
                        .customerId(UUID.nameUUIDFromBytes((tenantId + "-pnl-customer-1").getBytes(StandardCharsets.UTF_8)))
                        .customerName("Customer A")
                        .revenue(BigDecimal.valueOf(42000))
                        .expense(BigDecimal.valueOf(2000))
                        .netProfit(BigDecimal.valueOf(40000))
                        .build(),
                CustomerWisePnLReportResponse.CustomerWisePnLItem.builder()
                        .customerId(UUID.nameUUIDFromBytes((tenantId + "-pnl-customer-2").getBytes(StandardCharsets.UTF_8)))
                        .customerName("Customer B")
                        .revenue(BigDecimal.valueOf(35000))
                        .expense(BigDecimal.valueOf(1500))
                        .netProfit(BigDecimal.valueOf(33500))
                        .build()
        );

        String search = pageQuery != null ? pageQuery.search() : null;
        List<CustomerWisePnLReportResponse.CustomerWisePnLItem> filtered = allItems.stream()
                .filter(it -> SearchFilterUtil.matchesSearch(search,
                        it.customerName(),
                        it.customerId() != null ? it.customerId().toString() : null))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<CustomerWisePnLReportResponse.CustomerWisePnLItem> pagedItems = filtered.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new CustomerWisePnLReportResponse(
                period,
                pagedItems,
                new PageMetadata(page, size, totalElements, totalPages, page == 0, toIndex == totalElements, pagedItems.isEmpty())
        );
    }
}
