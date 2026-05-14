package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.ProductWisePnLReportResponse;
import com.ksa.financing.ledger.application.service.util.SearchFilterUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductWisePnLReportService {

    public ProductWisePnLReportResponse generate(UUID tenantId, String period, PageQuery pageQuery) {
        List<ProductWisePnLReportResponse.ProductWisePnLItem> allItems = List.of(
                ProductWisePnLReportResponse.ProductWisePnLItem.builder()
                        .productCode("MURABAHA")
                        .productName("Murabaha")
                        .revenue(BigDecimal.valueOf(120000))
                        .expense(BigDecimal.valueOf(15000))
                        .netProfit(BigDecimal.valueOf(105000))
                        .build(),
                ProductWisePnLReportResponse.ProductWisePnLItem.builder()
                        .productCode("IJARA")
                        .productName("Ijara")
                        .revenue(BigDecimal.valueOf(82000))
                        .expense(BigDecimal.valueOf(10000))
                        .netProfit(BigDecimal.valueOf(72000))
                        .build()
        );

        String search = pageQuery != null ? pageQuery.search() : null;
        List<ProductWisePnLReportResponse.ProductWisePnLItem> filtered = allItems.stream()
                .filter(it -> SearchFilterUtil.matchesSearch(search, it.productCode(), it.productName()))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery != null ? pageQuery.page() : 0;
        int size = pageQuery != null && pageQuery.size() > 0 ? pageQuery.size() : 20;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<ProductWisePnLReportResponse.ProductWisePnLItem> pagedItems = filtered.subList(fromIndex, toIndex);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return ProductWisePnLReportResponse.builder()
                .period(period)
                .items(pagedItems)
                .pagination(new PageMetadata(
                        page, size, totalElements, totalPages,
                        page == 0, toIndex == totalElements, pagedItems.isEmpty()))
                .build();
    }
}
