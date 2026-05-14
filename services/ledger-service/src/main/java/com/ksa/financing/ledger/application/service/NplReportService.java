package com.ksa.financing.ledger.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.NplReportResponse;
import com.ksa.financing.ledger.infrastructure.client.ReportDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NplReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportDataClient reportDataClient;

    public NplReportResponse generate(UUID tenantId, LocalDate asOfDate) {
        JsonNode overdueData = reportDataClient.fetchOverdueInstallments(asOfDate, 90, null);
        JsonNode overdueItems = overdueData.path("items");

        BigDecimal bucket90To179 = BigDecimal.ZERO;
        BigDecimal bucket180Plus = BigDecimal.ZERO;

        if (overdueItems.isArray()) {
            for (JsonNode item : overdueItems) {
                int daysPastDue = item.path("daysPastDue").asInt(0);
                BigDecimal totalOverdue = toBig(item, "totalOverdue");

                if (daysPastDue >= 180) {
                    bucket180Plus = bucket180Plus.add(totalOverdue);
                } else if (daysPastDue >= 90) {
                    bucket90To179 = bucket90To179.add(totalOverdue);
                }
            }
        }

        BigDecimal nplOutstanding = bucket90To179.add(bucket180Plus);

        JsonNode outstandingData = reportDataClient.fetchOutstandingBalances(asOfDate, null, null);
        BigDecimal totalOutstanding = totalOutstanding(outstandingData);
        BigDecimal nplRatio = calculateRatio(nplOutstanding, totalOutstanding);

        log.info("Generated NPL report tenant={} asOfDate={} totalOutstanding={} nplOutstanding={} nplRatio={}",
                tenantId, asOfDate, totalOutstanding, nplOutstanding, nplRatio);

        return NplReportResponse.builder()
                .asOfDate(asOfDate)
                .totalOutstanding(totalOutstanding)
                .nplOutstanding(nplOutstanding)
                .nplRatio(nplRatio)
                .buckets(List.of(
                        NplReportResponse.NplBucket.builder()
                                .bucket("90-179")
                                .outstanding(bucket90To179)
                                .build(),
                        NplReportResponse.NplBucket.builder()
                                .bucket("180+")
                                .outstanding(bucket180Plus)
                                .build()
                ))
                .build();
    }

    private BigDecimal totalOutstanding(JsonNode data) {
        BigDecimal totalFromTopLevel = toBig(data, "totalPrincipalOutstanding")
                .add(toBig(data, "totalProfitOutstanding"))
                .add(toBig(data, "totalFeesOutstanding"));

        if (totalFromTopLevel.compareTo(BigDecimal.ZERO) > 0) {
            return totalFromTopLevel;
        }

        JsonNode items = data.path("items");
        if (!items.isArray()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode item : items) {
            total = total
                    .add(toBig(item, "outstandingPrincipal"))
                    .add(toBig(item, "outstandingProfit"))
                    .add(toBig(item, "outstandingFees"));
        }
        return total;
    }

    private BigDecimal calculateRatio(BigDecimal nplOutstanding, BigDecimal totalOutstanding) {
        if (totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return nplOutstanding
                .multiply(HUNDRED)
                .divide(totalOutstanding, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBig(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(node.get(field).asText("0"));
    }
}
