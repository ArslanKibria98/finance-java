package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.OverdueLoanReportResponse;
import com.ksa.financing.ledger.application.dto.OverdueLoanReportResponse.Line;
import com.ksa.financing.ledger.infrastructure.client.ReportDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueLoanReportService {

    private final ReportDataClient reportDataClient;

    public OverdueLoanReportResponse generate(UUID tenantId,
                                              LocalDate asOfDate,
                                              Integer minDaysPastDue,
                                              String productCode,
                                              PageQuery pageQuery) {
        log.info("Generating overdue loan report tenant={} asOf={} minDPD={} product={} page={}",
                tenantId, asOfDate, minDaysPastDue, productCode, pageQuery.page());

        // We still fetch full results if productCode filter is present, 
        // because collections-service doesn't have productCode.
        // Otherwise, we pass pagination to collections-service.
        PageQuery internalQuery = (productCode == null || productCode.isBlank()) ? pageQuery : null;
        JsonNode data = reportDataClient.fetchOverdueInstallments(asOfDate, minDaysPastDue, internalQuery);
        
        var items = data.path("items");
        int totalElements = data.path("totalCount").asInt(0);
        BigDecimal totalOverdueSum = data.has("totalOverdueAmount") ? new BigDecimal(data.get("totalOverdueAmount").asText()) : BigDecimal.ZERO;

        if (!items.isArray() || items.isEmpty()) {
            return new OverdueLoanReportResponse(
                    asOfDate,
                    0,
                    BigDecimal.ZERO,
                    List.of(),
                    new PageMetadata(pageQuery.page(), pageQuery.size(), 0L, 0, true, true, true)
            );
        }

        // Enrich with loan details (loan number, customer, product) from lending-service
        List<UUID> loanIds = new ArrayList<>();
        for (JsonNode n : items) loanIds.add(UUID.fromString(n.get("loanId").asText()));

        JsonNode loans = reportDataClient.fetchLoanLookup(loanIds);
        Map<UUID, JsonNode> loanMap = new HashMap<>();
        if (loans != null && loans.isArray()) {
            for (JsonNode l : loans) {
                loanMap.put(UUID.fromString(l.get("loanId").asText()), l);
            }
        }

        String searchTerm = lowerSearch(pageQuery.search());
        List<Line> allLines = new ArrayList<>();
        BigDecimal currentTotal = BigDecimal.ZERO;
        
        for (JsonNode n : items) {
            UUID loanId = UUID.fromString(n.get("loanId").asText());
            JsonNode loan = loanMap.get(loanId);

            String loanNumber = loan != null ? loan.path("loanNumber").asText(null) : null;
            String prodCode = loan != null ? loan.path("productCode").asText(null) : null;
            UUID customerId = loan != null && loan.hasNonNull("customerId")
                    ? UUID.fromString(loan.get("customerId").asText()) : null;
            String loanStatus = loan != null ? loan.path("status").asText(null) : null;
            String dpdBucket = n.path("dpdBucket").asText(null);

            if (productCode != null && !productCode.isBlank() && !productCode.equals(prodCode)) continue;
            
            // If we didn't pass search to internal API, we filter here.
            // But we already passed it if productCode is null.
            if (internalQuery == null && !matchesSearch(searchTerm, loanNumber, prodCode, loanStatus, dpdBucket,
                    customerId != null ? customerId.toString() : null,
                    loanId.toString())) continue;

            BigDecimal principalOverdue = toBig(n, "principalOverdue");
            BigDecimal profitOverdue = toBig(n, "profitOverdue");
            BigDecimal penalty = toBig(n, "penaltyAmount");
            BigDecimal totalOverdue = toBig(n, "totalOverdue");

            allLines.add(Line.builder()
                    .loanAccountNumber(loanNumber != null ? loanNumber : loanId.toString())
                    .customerId(customerId)
                    .customerName(null)
                    .nationalId(null)
                    .productName(prodCode)
                    .principalOverdue(principalOverdue)
                    .profitOverdue(profitOverdue)
                    .penaltyAmount(penalty)
                    .totalOverdue(totalOverdue)
                    .daysPastDue(n.path("daysPastDue").asInt(0))
                    .dpdBucket(dpdBucket)
                    .oldestUnpaidDate(LocalDate.parse(n.path("oldestUnpaidDate").asText()))
                    .status(loanStatus)
                    .build());
            currentTotal = currentTotal.add(totalOverdue);
        }

        List<Line> pagedItems;
        PageMetadata metadata;

        if (internalQuery != null) {
            // Already paged by internal API
            pagedItems = allLines;
            int totalPages = (int) Math.ceil((double) totalElements / pageQuery.size());
            metadata = new PageMetadata(pageQuery.page(), pageQuery.size(), totalElements, totalPages, 
                    pageQuery.page() == 0, (pageQuery.page() + 1) >= totalPages, pagedItems.isEmpty());
        } else {
            // Manual paging needed because we fetched everything for productCode filtering
            int filteredTotal = allLines.size();
            int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), filteredTotal);
            int toIndex = Math.min(fromIndex + pageQuery.size(), filteredTotal);
            pagedItems = allLines.subList(fromIndex, toIndex);
            int totalPages = (int) Math.ceil((double) filteredTotal / pageQuery.size());
            metadata = new PageMetadata(pageQuery.page(), pageQuery.size(), filteredTotal, totalPages, 
                    pageQuery.page() == 0, toIndex == filteredTotal, pagedItems.isEmpty());
            totalOverdueSum = currentTotal;
        }

        return new OverdueLoanReportResponse(
                asOfDate,
                (int)metadata.totalElements(),
                totalOverdueSum,
                pagedItems,
                metadata
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
