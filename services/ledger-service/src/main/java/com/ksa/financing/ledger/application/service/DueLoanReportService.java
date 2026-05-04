package com.ksa.financing.ledger.application.service;

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

    public DueLoanReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        log.info("Generating due loan report tenant={} from={} to={}", tenantId, fromDate, toDate);

        JsonNode data = reportDataClient.fetchDueInstallments(fromDate, toDate);
        var items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return DueLoanReportResponse.builder()
                    .fromDate(fromDate).toDate(toDate).totalCount(0)
                    .totalDueAmount(BigDecimal.ZERO).items(List.of()).build();
        }

        List<UUID> loanIds = new ArrayList<>();
        for (JsonNode n : items) loanIds.add(UUID.fromString(n.get("loanId").asText()));
        JsonNode loans = reportDataClient.fetchLoanLookup(loanIds);
        Map<UUID, JsonNode> loanMap = new HashMap<>();
        if (loans != null && loans.isArray()) {
            for (JsonNode l : loans) loanMap.put(UUID.fromString(l.get("loanId").asText()), l);
        }

        List<Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode n : items) {
            UUID loanId = UUID.fromString(n.get("loanId").asText());
            JsonNode loan = loanMap.get(loanId);
            BigDecimal amount = toBig(n, "installmentAmount");

            lines.add(Line.builder()
                    .loanAccountNumber(loan != null ? loan.path("loanNumber").asText(null) : loanId.toString())
                    .customerId(loan != null && loan.hasNonNull("customerId")
                            ? UUID.fromString(loan.get("customerId").asText()) : null)
                    .customerName(null)
                    .productName(loan != null ? loan.path("productCode").asText(null) : null)
                    .installmentNumber(n.path("installmentNumber").asInt())
                    .dueDate(LocalDate.parse(n.path("dueDate").asText()))
                    .principalDue(toBig(n, "principalDue"))
                    .profitDue(toBig(n, "profitDue"))
                    .installmentAmount(amount)
                    .daysUntilDue(n.path("daysUntilDue").asInt(0))
                    .status(n.path("status").asText(null))
                    .build());
            total = total.add(amount);
        }

        return DueLoanReportResponse.builder()
                .fromDate(fromDate).toDate(toDate)
                .totalCount(lines.size()).totalDueAmount(total).items(lines).build();
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }
}
