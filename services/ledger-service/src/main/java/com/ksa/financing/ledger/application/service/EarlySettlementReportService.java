package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.EarlySettlementReportResponse;
import com.ksa.financing.ledger.application.dto.EarlySettlementReportResponse.EarlySettlementItem;
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
public class EarlySettlementReportService {

    private final ReportDataClient reportDataClient;

    public EarlySettlementReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate, PageQuery pageQuery) {
        log.info("Generating early settlement report tenant={} from={} to={} page={}",
                tenantId, fromDate, toDate, pageQuery.page());

        JsonNode data = reportDataClient.fetchEarlySettlements(fromDate, toDate, pageQuery);
        String searchTerm = lowerSearch(pageQuery.search());
        List<EarlySettlementItem> allItems = new ArrayList<>();
        if (data != null && data.isArray()) {
            for (JsonNode n : data) {
                UUID loanId = UUID.fromString(n.get("loanId").asText());
                UUID custId = n.hasNonNull("customerId") ? UUID.fromString(n.get("customerId").asText()) : null;
                if (!matchesSearch(searchTerm, loanId.toString(),
                        custId != null ? custId.toString() : null)) continue;

                allItems.add(EarlySettlementItem.builder()
                        .loanId(loanId)
                        .customerId(custId)
                        .settlementDate(LocalDate.parse(n.get("settlementDate").asText()))
                        .outstandingAmount(toBig(n, "outstandingAmount"))
                        .rebateAmount(toBig(n, "rebateAmount"))
                        .build());
            }
        }

        int totalElements = allItems.size();
        int page = pageQuery.page();
        int size = pageQuery.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<EarlySettlementItem> pagedItems = allItems.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new EarlySettlementReportResponse(
                fromDate,
                toDate,
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
