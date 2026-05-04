package com.ksa.financing.ledger.application.service;

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
                                                         String productCode) {
        log.info("Generating loan balance outstanding report tenant={} asOf={} customer={} product={}",
                tenantId, asOfDate, customerId, productCode);

        JsonNode data = reportDataClient.fetchOutstandingBalances(asOfDate, productCode);
        var items = data.path("items");
        LocalDate reportDate = data.hasNonNull("asOfDate")
                ? LocalDate.parse(data.get("asOfDate").asText())
                : (asOfDate != null ? asOfDate : LocalDate.now());

        List<Line> lines = new ArrayList<>();
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        if (items.isArray()) {
            for (JsonNode n : items) {
                UUID custId = n.hasNonNull("customerId")
                        ? UUID.fromString(n.get("customerId").asText()) : null;
                if (customerId != null && !customerId.equals(custId)) continue;

                BigDecimal principal = toBig(n, "outstandingPrincipal");
                BigDecimal profit = toBig(n, "outstandingProfit");
                BigDecimal fees = toBig(n, "outstandingFees");

                lines.add(Line.builder()
                        .loanAccountNumber(n.path("loanNumber").asText(null))
                        .customerId(custId)
                        .customerName(null)
                        .productName(n.path("productCode").asText(null))
                        .disbursedAmount(toBig(n, "disbursedAmount"))
                        .totalPaid(toBig(n, "totalPaid"))
                        .principalOutstanding(principal)
                        .profitOutstanding(profit)
                        .penaltiesOutstanding(fees)
                        .nextDueDate(null)
                        .nextDueAmount(BigDecimal.ZERO)
                        .loanStatus(n.path("status").asText(null))
                        .build());
                totalPrincipal = totalPrincipal.add(principal);
                totalProfit = totalProfit.add(profit);
                totalFees = totalFees.add(fees);
            }
        }

        return LoanBalanceOutstandingReportResponse.builder()
                .asOfDate(reportDate)
                .totalCount(lines.size())
                .totalPrincipalOutstanding(totalPrincipal)
                .totalProfitOutstanding(totalProfit)
                .totalPenaltiesOutstanding(totalFees)
                .items(lines)
                .build();
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }
}
