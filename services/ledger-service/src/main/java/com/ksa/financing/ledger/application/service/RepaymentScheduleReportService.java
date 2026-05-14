package com.ksa.financing.ledger.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.RepaymentScheduleListResponse;
import com.ksa.financing.ledger.application.dto.RepaymentScheduleReportResponse;
import com.ksa.financing.ledger.application.dto.RepaymentScheduleReportResponse.Installment;
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
public class RepaymentScheduleReportService {

    private final ReportDataClient reportDataClient;

    public RepaymentScheduleReportResponse generate(UUID tenantId, UUID loanId) {
        log.info("Generating repayment schedule report tenant={} loanId={}", tenantId, loanId);

        JsonNode installmentsJson = reportDataClient.fetchScheduleInstallments(loanId);
        JsonNode loanJson = reportDataClient.fetchLoanLookup(List.of(loanId));
        JsonNode loan = loanJson != null && loanJson.isArray() && !loanJson.isEmpty()
                ? loanJson.get(0) : null;

        List<Installment> items = new ArrayList<>();
        BigDecimal principalSum = BigDecimal.ZERO;
        BigDecimal profitSum = BigDecimal.ZERO;
        BigDecimal remainingPrincipal = loan != null && loan.hasNonNull("principalAmount")
                ? new BigDecimal(loan.get("principalAmount").asText("0")) : BigDecimal.ZERO;

        if (installmentsJson != null && installmentsJson.isArray()) {
            for (JsonNode n : installmentsJson) {
                BigDecimal principal = toBig(n, "principalDue");
                BigDecimal profit = toBig(n, "profitDue");
                BigDecimal amount = toBig(n, "installmentAmount");
                remainingPrincipal = remainingPrincipal.subtract(principal);

                items.add(Installment.builder()
                        .installmentNumber(n.path("installmentNumber").asInt())
                        .principalDue(principal)
                        .profitDue(profit)
                        .installmentAmount(amount)
                        .remainingPrincipal(remainingPrincipal)
                        .penaltyAmount(toBig(n, "penaltyAmount"))
                        .dueDate(LocalDate.parse(n.path("dueDate").asText()))
                        .status(n.path("status").asText())
                        .paymentDate(n.hasNonNull("paymentDate")
                                ? LocalDate.parse(n.get("paymentDate").asText()) : null)
                        .amountPaid(toBig(n, "amountPaid"))
                        .build());
                principalSum = principalSum.add(principal);
                profitSum = profitSum.add(profit);
            }
        }

        return RepaymentScheduleReportResponse.builder()
                .loanId(loanId)
                .loanAccountNumber(loan != null ? loan.path("loanNumber").asText(null) : null)
                .customerName(null)
                .productName(loan != null ? loan.path("productCode").asText(null) : null)
                .disbursedPrincipal(loan != null ? toBig(loan, "principalAmount") : principalSum)
                .totalProfit(loan != null ? toBig(loan, "profitAmount") : profitSum)
                .totalPayable(principalSum.add(profitSum))
                .tenureMonths(loan != null ? loan.path("tenureMonths").asInt(items.size()) : items.size())
                .installments(items)
                .build();
    }

    public List<RepaymentScheduleReportResponse> generateAll(
            UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        log.info("Generating bulk repayment schedule report tenant={} from={} to={}",
                tenantId, fromDate, toDate);

        // 1. Find all loans disbursed in the window via lending-service.
        JsonNode disbursed = reportDataClient.fetchDisbursedLoans(fromDate, toDate, null, null);
        if (disbursed == null || !disbursed.isArray() || disbursed.isEmpty()) {
            return List.of();
        }

        // 2. For each loan, fetch its schedule (sourced from collections-service installments).
        List<RepaymentScheduleReportResponse> out = new ArrayList<>();
        for (JsonNode loanNode : disbursed) {
            if (!loanNode.hasNonNull("loanId")) continue;
            UUID loanId;
            try {
                loanId = UUID.fromString(loanNode.get("loanId").asText());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            try {
                out.add(generate(tenantId, loanId));
            } catch (RuntimeException ex) {
                log.warn("Skipping loan {} in bulk schedule report: {}", loanId, ex.getMessage());
            }
        }
        return out;
    }

    public RepaymentScheduleListResponse generateAllPaged(
            UUID tenantId, LocalDate fromDate, LocalDate toDate, PageQuery pageQuery) {
        List<RepaymentScheduleReportResponse> all = generateAll(tenantId, fromDate, toDate);

        String search = pageQuery != null ? pageQuery.search() : null;
        List<RepaymentScheduleReportResponse> filtered = all.stream()
                .filter(it -> com.ksa.financing.ledger.application.service.util.SearchFilterUtil.matchesSearch(search,
                        it.loanId() != null ? it.loanId().toString() : null,
                        it.loanAccountNumber(),
                        it.customerName(),
                        it.productName()))
                .toList();

        int totalElements = filtered.size();
        int page = pageQuery != null ? pageQuery.page() : 0;
        int size = pageQuery != null && pageQuery.size() > 0 ? pageQuery.size() : 20;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<RepaymentScheduleReportResponse> paged = filtered.subList(fromIndex, toIndex);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return RepaymentScheduleListResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .items(paged)
                .pagination(new PageMetadata(
                        page, size, totalElements, totalPages,
                        page == 0, toIndex == totalElements, paged.isEmpty()))
                .build();
    }

    private BigDecimal toBig(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText("0")) : BigDecimal.ZERO;
    }
}
