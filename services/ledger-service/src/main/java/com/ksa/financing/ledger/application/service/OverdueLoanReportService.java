package com.ksa.financing.ledger.application.service;

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
                                              String productCode) {
        log.info("Generating overdue loan report tenant={} asOf={} minDPD={} product={}",
                tenantId, asOfDate, minDaysPastDue, productCode);

        JsonNode data = reportDataClient.fetchOverdueInstallments(asOfDate, minDaysPastDue);
        var items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return OverdueLoanReportResponse.builder()
                    .asOfDate(asOfDate)
                    .totalCount(0)
                    .totalOverdueAmount(BigDecimal.ZERO)
                    .items(List.of())
                    .build();
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

        List<Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode n : items) {
            UUID loanId = UUID.fromString(n.get("loanId").asText());
            JsonNode loan = loanMap.get(loanId);

            String loanNumber = loan != null ? loan.path("loanNumber").asText(null) : null;
            String prodCode = loan != null ? loan.path("productCode").asText(null) : null;
            UUID customerId = loan != null && loan.hasNonNull("customerId")
                    ? UUID.fromString(loan.get("customerId").asText()) : null;
            String loanStatus = loan != null ? loan.path("status").asText(null) : null;

            if (productCode != null && !productCode.isBlank() && !productCode.equals(prodCode)) continue;

            BigDecimal principalOverdue = toBig(n, "principalOverdue");
            BigDecimal profitOverdue = toBig(n, "profitOverdue");
            BigDecimal penalty = toBig(n, "penaltyAmount");
            BigDecimal totalOverdue = toBig(n, "totalOverdue");

            lines.add(Line.builder()
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
                    .dpdBucket(n.path("dpdBucket").asText(null))
                    .oldestUnpaidDate(LocalDate.parse(n.path("oldestUnpaidDate").asText()))
                    .status(loanStatus)
                    .build());
            total = total.add(totalOverdue);
        }

        return OverdueLoanReportResponse.builder()
                .asOfDate(asOfDate)
                .totalCount(lines.size())
                .totalOverdueAmount(total)
                .items(lines)
                .build();
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }
}
