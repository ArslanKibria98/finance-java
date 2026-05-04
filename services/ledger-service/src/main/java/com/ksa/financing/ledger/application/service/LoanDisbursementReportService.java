package com.ksa.financing.ledger.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.ledger.application.dto.LoanDisbursementReportResponse;
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
public class LoanDisbursementReportService {

    private final ReportDataClient reportDataClient;

    public LoanDisbursementReportResponse generate(UUID tenantId,
                                                   LocalDate fromDate,
                                                   LocalDate toDate,
                                                   String productCode,
                                                   String branchOrChannel,
                                                   String status) {
        log.debug("Generating loan disbursement report tenant={} from={} to={} product={} branch={} status={}",
                tenantId, fromDate, toDate, productCode, branchOrChannel, status);

        // Fetch full range then apply tolerant product filter locally,
        // because UI labels (e.g. MICROFINANCE) may not match DB code/name exactly.
        JsonNode data = reportDataClient.fetchDisbursedLoans(fromDate, toDate, null);
        if (data == null || !data.isArray() || data.isEmpty()) {
            return emptyResponse(fromDate, toDate);
        }

        var statusFilter = normalize(status);
        var branchFilter = normalize(branchOrChannel);
        var productFilter = normalize(productCode);
        var lines = new ArrayList<LoanDisbursementReportResponse.Line>();
        var total = BigDecimal.ZERO;

        for (JsonNode node : data) {
            var itemProductCode = text(node, "productCode");
            var itemProductName = text(node, "productName");
            if (!matchesProduct(productFilter, itemProductCode, itemProductName)) {
                continue;
            }

            var loanStatus = text(node, "status");
            if (!matchesStatus(statusFilter, loanStatus)) {
                continue;
            }

            var sourceBranch = text(node, "branchOrChannel");
            if (!matchesBranch(branchFilter, sourceBranch)) {
                continue;
            }

            var amount = toBig(node, "disbursedAmount");
            var line = LoanDisbursementReportResponse.Line.builder()
                    .applicationNumber(text(node, "applicationNumber"))
                    .loanAccountNumber(text(node, "loanNumber"))
                    .customerId(uuid(node, "customerId"))
                    .customerName(null)
                    .nationalId(text(node, "nationalId"))
                    .productCode(text(node, "productCode"))
                    .productName(text(node, "productName"))
                    .disbursementDate(date(node, "disbursementDate"))
                    .disbursedAmount(amount)
                    .tenureMonths(node.hasNonNull("tenureMonths") ? node.get("tenureMonths").asInt() : null)
                    .status(loanStatus)
                    .branchOrChannel(sourceBranch)
                    .build();

            lines.add(line);
            total = total.add(amount);
        }

        return LoanDisbursementReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalCount(lines.size())
                .totalDisbursedAmount(total)
                .items(lines)
                .build();
    }

    private LoanDisbursementReportResponse emptyResponse(LocalDate fromDate, LocalDate toDate) {
        return LoanDisbursementReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalCount(0)
                .totalDisbursedAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }

    private boolean matchesStatus(String statusFilter, String actualStatus) {
        if (statusFilter == null) {
            return true;
        }
        var normalizedActual = normalize(actualStatus);
        if (normalizedActual == null) {
            return false;
        }
        if (normalizedActual.equals(statusFilter)) {
            return true;
        }
        // UI often sends DISBURSED while lending loan status is ACTIVE after disbursement.
        return "DISBURSED".equals(statusFilter) && "ACTIVE".equals(normalizedActual);
    }

    private boolean matchesBranch(String branchFilter, String actualBranchOrChannel) {
        if (branchFilter == null || "ALL".equals(branchFilter)) {
            return true;
        }
        var normalizedActual = normalize(actualBranchOrChannel);
        if (normalizedActual == null) {
            // Source data does not always carry branch/channel; do not hide valid disbursements.
            return true;
        }
        return normalizedActual.equals(branchFilter);
    }

    private boolean matchesProduct(String productFilter, String productCode, String productName) {
        if (productFilter == null || "ALL".equals(productFilter)) {
            return true;
        }
        var normalizedCode = normalizeToken(productCode);
        var normalizedName = normalizeToken(productName);
        var normalizedFilter = normalizeToken(productFilter);
        if (normalizedFilter == null) {
            return true;
        }
        return fuzzyContains(normalizedCode, normalizedFilter)
                || fuzzyContains(normalizedName, normalizedFilter);
    }

    private boolean fuzzyContains(String source, String filter) {
        if (source == null || filter == null) {
            return false;
        }
        if (source.contains(filter) || filter.contains(source)) {
            return true;
        }
        var minPrefix = Math.min(8, Math.min(source.length(), filter.length()));
        if (minPrefix < 4) {
            return false;
        }
        return source.substring(0, minPrefix).equals(filter.substring(0, minPrefix));
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private UUID uuid(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return UUID.fromString(node.get(field).asText());
    }

    private LocalDate date(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return LocalDate.parse(node.get(field).asText());
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeToken(String value) {
        var normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("[^A-Z0-9]", "");
    }
}
