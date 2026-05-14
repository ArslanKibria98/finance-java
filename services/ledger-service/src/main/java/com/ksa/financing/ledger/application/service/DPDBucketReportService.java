package com.ksa.financing.ledger.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.DPDBucketDto;
import com.ksa.financing.ledger.application.dto.DPDBucketReportResponse;
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
public class DPDBucketReportService {

    private static final List<int[]> BUCKET_RANGES = List.of(
            new int[]{0, 0},      // Current
            new int[]{1, 30},     // Grace
            new int[]{31, 60},    // Mild
            new int[]{61, 90},    // High
            new int[]{91, Integer.MAX_VALUE}  // Defaulted
    );

    private static final List<String> BUCKET_LABELS = List.of(
            "0 (Current)", "1-30", "31-60", "61-90", "90+"
    );

    private final ReportDataClient reportDataClient;

    public DPDBucketReportResponse generate(UUID tenantId, LocalDate date) {
        log.info("Generating DPD bucket report: tenantId={} date={}", tenantId, date);

        var overdue = reportDataClient.fetchOverdueInstallments(date, 0, null);

        Map<Integer, Integer> countByBucket = new HashMap<>();
        Map<Integer, BigDecimal> amountByBucket = new HashMap<>();
        for (int i = 0; i < BUCKET_RANGES.size(); i++) {
            countByBucket.put(i, 0);
            amountByBucket.put(i, BigDecimal.ZERO);
        }

        if (overdue != null && overdue.isArray()) {
            for (JsonNode node : overdue) {
                int dpd = intValue(node, "daysPastDue");
                int bucketIdx = bucketFor(dpd);
                BigDecimal amount = decimalValue(node, "totalAmount");
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    amount = decimalValue(node, "outstandingAmount");
                }
                countByBucket.merge(bucketIdx, 1, Integer::sum);
                amountByBucket.merge(bucketIdx, amount, BigDecimal::add);
            }
        }

        var buckets = new ArrayList<DPDBucketDto>();
        for (int i = 0; i < BUCKET_RANGES.size(); i++) {
            buckets.add(DPDBucketDto.builder()
                    .dpdRange(BUCKET_LABELS.get(i))
                    .loanCount(countByBucket.get(i))
                    .totalAmount(amountByBucket.get(i))
                    .build());
        }

        return DPDBucketReportResponse.builder()
                .reportDate(date)
                .buckets(buckets)
                .build();
    }

    private int bucketFor(int dpd) {
        for (int i = 0; i < BUCKET_RANGES.size(); i++) {
            var range = BUCKET_RANGES.get(i);
            if (dpd >= range[0] && dpd <= range[1]) return i;
        }
        return BUCKET_RANGES.size() - 1;
    }

    private int intValue(JsonNode node, String field) {
        var v = node.get(field);
        return v == null || v.isNull() ? 0 : v.asInt(0);
    }

    private BigDecimal decimalValue(JsonNode node, String field) {
        var v = node.get(field);
        if (v == null || v.isNull()) return BigDecimal.ZERO;
        return new BigDecimal(v.asText("0"));
    }
}
