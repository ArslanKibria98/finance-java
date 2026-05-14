package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.LoanBalanceOutstandingReportResponse;
import com.ksa.financing.ledger.application.dto.LoanBalanceOutstandingReportResponse.Line;
import com.ksa.financing.ledger.infrastructure.client.ReportDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanBalanceOutstandingReportService {

    private final ReportDataClient reportDataClient;

    public LoanBalanceOutstandingReportResponse generate(UUID tenantId,
                                                         LocalDate asOfDate,
                                                         UUID customerId,
                                                         String productCode,
                                                         PageQuery pageQuery) {
        log.info("Generating loan balance outstanding report tenant={} asOf={} customer={} product={} page={}",
                tenantId, asOfDate, customerId, productCode, pageQuery.page());

        JsonNode data = reportDataClient.fetchOutstandingBalances(asOfDate, productCode, pageQuery);
        var items = data.path("items");
        LocalDate reportDate = data.hasNonNull("asOfDate")
                ? LocalDate.parse(data.get("asOfDate").asText())
                : (asOfDate != null ? asOfDate : LocalDate.now());

        List<Line> allLines = new ArrayList<>();
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        String searchTerm = lowerSearch(pageQuery.search());
        if (items.isArray()) {
            for (JsonNode n : items) {
                UUID custId = n.hasNonNull("customerId")
                        ? UUID.fromString(n.get("customerId").asText()) : null;
                if (customerId != null && !customerId.equals(custId)) continue;

                String loanNumber = n.path("loanNumber").asText(null);
                String prodCode = n.path("productCode").asText(null);
                String status = n.path("status").asText(null);
                if (!matchesSearch(searchTerm, loanNumber, prodCode, status,
                        custId != null ? custId.toString() : null)) continue;

                BigDecimal principal = toBig(n, "outstandingPrincipal");
                BigDecimal profit = toBig(n, "outstandingProfit");
                BigDecimal fees = toBig(n, "outstandingFees");

                allLines.add(Line.builder()
                        .loanAccountNumber(loanNumber)
                        .customerId(custId)
                        .customerName(null)
                        .productName(prodCode)
                        .disbursedAmount(toBig(n, "disbursedAmount"))
                        .totalPaid(toBig(n, "totalPaid"))
                        .principalOutstanding(principal)
                        .profitOutstanding(profit)
                        .penaltiesOutstanding(fees)
                        .nextDueDate(null)
                        .nextDueAmount(BigDecimal.ZERO)
                        .loanStatus(status)
                        .build());
                totalPrincipal = totalPrincipal.add(principal);
                totalProfit = totalProfit.add(profit);
                totalFees = totalFees.add(fees);
            }
        }

        int totalElements = allLines.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Line> pagedItems = allLines.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new LoanBalanceOutstandingReportResponse(
                reportDate,
                totalElements,
                totalPrincipal,
                totalProfit,
                totalFees,
                pagedItems,
                new PageMetadata(page, size, totalElements, totalPages, page == 0, toIndex == totalElements, pagedItems.isEmpty())
        );
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }

    private String lowerSearch(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().toLowerCase();
    }

    private boolean matchesSearch(String term, String... fields) {
        if (term == null) return true;
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(term)) return true;
        }
        return false;
    }
}
