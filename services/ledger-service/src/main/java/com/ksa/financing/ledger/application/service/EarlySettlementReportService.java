package com.ksa.financing.ledger.application.service;

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

    public EarlySettlementReportResponse generate(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        log.info("Generating early settlement report tenant={} from={} to={}", tenantId, fromDate, toDate);

        JsonNode data = reportDataClient.fetchEarlySettlements(fromDate, toDate);
        List<EarlySettlementItem> items = new ArrayList<>();
        if (data != null && data.isArray()) {
            for (JsonNode n : data) {
                items.add(EarlySettlementItem.builder()
                        .loanId(UUID.fromString(n.get("loanId").asText()))
                        .customerId(n.hasNonNull("customerId")
                                ? UUID.fromString(n.get("customerId").asText()) : null)
                        .settlementDate(LocalDate.parse(n.get("settlementDate").asText()))
                        .outstandingAmount(toBig(n, "outstandingAmount"))
                        .rebateAmount(toBig(n, "rebateAmount"))
                        .build());
            }
        }

        return EarlySettlementReportResponse.builder()
                .fromDate(fromDate).toDate(toDate).items(items).build();
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }
}
