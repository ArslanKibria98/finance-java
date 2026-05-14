package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.DueLoanReportResponse;
import com.ksa.financing.ledger.application.dto.DueLoanReportResponse.Line;
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
public class DueLoanReportService {

    private final ReportDataClient reportDataClient;

    public DueLoanReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate, PageQuery pageQuery) {
        log.info("Generating due loan report tenant={} from={} to={} page={}",
                tenantId, fromDate, toDate, pageQuery.page());

        JsonNode data = reportDataClient.fetchDueInstallments(fromDate, toDate, pageQuery);
        var items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return new DueLoanReportResponse(
                    fromDate, toDate, 0, BigDecimal.ZERO, List.of(),
                    new PageMetadata(pageQuery.page(), pageQuery.size(), 0L, 0, true, true, true)
            );
        }

        List<UUID> loanIds = new ArrayList<>();
        for (JsonNode n : items) loanIds.add(UUID.fromString(n.get("loanId").asText()));
        JsonNode loans = reportDataClient.fetchLoanLookup(loanIds);
        Map<UUID, JsonNode> loanMap = new HashMap<>();
        if (loans != null && loans.isArray()) {
            for (JsonNode l : loans) loanMap.put(UUID.fromString(l.get("loanId").asText()), l);
        }

        String searchTerm = lowerSearch(pageQuery.search());
        List<Line> allLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode n : items) {
            UUID loanId = UUID.fromString(n.get("loanId").asText());
            JsonNode loan = loanMap.get(loanId);
            BigDecimal amount = toBig(n, "installmentAmount");

            String loanNumber = loan != null ? loan.path("loanNumber").asText(null) : null;
            String prodCode = loan != null ? loan.path("productCode").asText(null) : null;
            String status = n.path("status").asText(null);
            UUID customerId = loan != null && loan.hasNonNull("customerId")
                    ? UUID.fromString(loan.get("customerId").asText()) : null;

            if (!matchesSearch(searchTerm, loanNumber, prodCode, status,
                    customerId != null ? customerId.toString() : null,
                    loanId.toString())) continue;

            allLines.add(Line.builder()
                    .loanAccountNumber(loanNumber != null ? loanNumber : loanId.toString())
                    .customerId(customerId)
                    .customerName(null)
                    .productName(prodCode)
                    .installmentNumber(n.path("installmentNumber").asInt())
                    .dueDate(LocalDate.parse(n.path("dueDate").asText()))
                    .principalDue(toBig(n, "principalDue"))
                    .profitDue(toBig(n, "profitDue"))
                    .installmentAmount(amount)
                    .daysUntilDue(n.path("daysUntilDue").asInt(0))
                    .status(status)
                    .build());
            total = total.add(amount);
        }

        int totalElements = allLines.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Line> pagedItems = allLines.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new DueLoanReportResponse(
                fromDate, toDate, totalElements, total, pagedItems,
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
