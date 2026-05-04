package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregation endpoints for loan-lifecycle reports backed by real installments + settlements.
 * Consumed by ledger-service report services (reports are UI-facing at /ledger-service/api/v1/reports/*).
 */
@RestController
@RequestMapping("/api/v1/internal/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collections Reports", description = "Internal loan-lifecycle report data (installments, settlements)")
public class CollectionsReportsController {

    private final JdbcTemplate jdbcTemplate;

    // ══════════════════════════════════════════════════════════════
    // OVERDUE INSTALLMENTS (aggregated per loan)
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "collections.reports", act = "read")
    @GetMapping("/overdue-installments")
    @Operation(summary = "Overdue installments aggregated by loan (source of truth for Overdue Loan Report)")
    public ResponseEntity<OverdueInstallmentsResponse> overdueInstallments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false, defaultValue = "1") Integer minDaysPastDue,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var cutoff = asOfDate.minusDays(Math.max(0, minDaysPastDue - 1));

        var sql = """
                SELECT  i.loan_id,
                        MIN(i.due_date)                          AS oldest_unpaid,
                        COALESCE(SUM(i.principal_amount - i.paid_principal), 0) AS principal_overdue,
                        COALESCE(SUM(i.profit_amount - i.paid_profit), 0)       AS profit_overdue,
                        COALESCE(SUM(i.late_penalty_amount - i.waived_penalty_amount), 0) AS penalty_amount,
                        COALESCE(SUM(i.total_amount - i.paid_total), 0)         AS total_overdue
                  FROM  installments i
                 WHERE  i.tenant_id = ?
                   AND  i.due_date <= ?
                   AND  i.status IN ('OVERDUE', 'DUE', 'SCHEDULED', 'PARTIALLY_PAID')
                   AND  (i.total_amount - i.paid_total) > 0
                 GROUP  BY i.loan_id
                HAVING  MIN(i.due_date) <= ?
                """;

        var rows = jdbcTemplate.query(sql,
                (rs, rn) -> new OverdueAggRow(
                        UUID.fromString(rs.getString("loan_id")),
                        rs.getDate("oldest_unpaid").toLocalDate(),
                        rs.getBigDecimal("principal_overdue"),
                        rs.getBigDecimal("profit_overdue"),
                        rs.getBigDecimal("penalty_amount"),
                        rs.getBigDecimal("total_overdue")
                ),
                tenantId, Date.valueOf(asOfDate), Date.valueOf(cutoff));

        var items = new ArrayList<OverdueItem>();
        BigDecimal total = BigDecimal.ZERO;
        for (var r : rows) {
            int dpd = (int) ChronoUnit.DAYS.between(r.oldestUnpaid(), asOfDate);
            if (dpd < minDaysPastDue) continue;
            items.add(new OverdueItem(
                    r.loanId(), r.oldestUnpaid(), r.principalOverdue(),
                    r.profitOverdue(), r.penaltyAmount(), r.totalOverdue(),
                    dpd, dpdBucket(dpd)));
            total = total.add(r.totalOverdue());
        }

        log.info("Overdue report: tenant={} asOf={} minDPD={} count={} total={}",
                tenantId, asOfDate, minDaysPastDue, items.size(), total);
        return ResponseEntity.ok(new OverdueInstallmentsResponse(asOfDate, items.size(), total, items));
    }

    // ══════════════════════════════════════════════════════════════
    // DUE INSTALLMENTS (window)
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "collections.reports", act = "read")
    @GetMapping("/due-installments")
    @Operation(summary = "Installments due within a window (for Due Loan Report)")
    public ResponseEntity<DueInstallmentsResponse> dueInstallments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var sql = """
                SELECT  i.loan_id,
                        i.installment_number,
                        i.due_date,
                        (i.principal_amount - i.paid_principal) AS principal_due,
                        (i.profit_amount - i.paid_profit)       AS profit_due,
                        (i.total_amount - i.paid_total)         AS amount_due,
                        i.status
                  FROM  installments i
                 WHERE  i.tenant_id = ?
                   AND  i.due_date BETWEEN ? AND ?
                   AND  i.status IN ('SCHEDULED', 'DUE', 'PARTIALLY_PAID', 'OVERDUE')
                   AND  (i.total_amount - i.paid_total) > 0
                 ORDER  BY i.due_date, i.loan_id, i.installment_number
                """;

        var today = LocalDate.now(ZoneOffset.UTC);
        var items = new ArrayList<DueItem>();
        BigDecimal total = BigDecimal.ZERO;

        var rows = jdbcTemplate.query(sql,
                (rs, rn) -> new Object[]{
                        UUID.fromString(rs.getString("loan_id")),
                        rs.getInt("installment_number"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getBigDecimal("principal_due"),
                        rs.getBigDecimal("profit_due"),
                        rs.getBigDecimal("amount_due"),
                        rs.getString("status")
                },
                tenantId, Date.valueOf(fromDate), Date.valueOf(toDate));

        for (var r : rows) {
            var due = (LocalDate) r[2];
            var amount = (BigDecimal) r[5];
            items.add(new DueItem(
                    (UUID) r[0], (Integer) r[1], due,
                    (BigDecimal) r[3], (BigDecimal) r[4], amount,
                    (int) ChronoUnit.DAYS.between(today, due), (String) r[6]));
            total = total.add(amount);
        }

        return ResponseEntity.ok(new DueInstallmentsResponse(fromDate, toDate, items.size(), total, items));
    }

    // ══════════════════════════════════════════════════════════════
    // REPAYMENT SCHEDULE
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "collections.reports", act = "read")
    @GetMapping("/schedule-installments")
    @Operation(summary = "Full installment plan for a specific loan")
    public ResponseEntity<List<ScheduleInstallment>> scheduleInstallments(
            @RequestParam UUID loanId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var sql = """
                SELECT  installment_number, principal_amount, profit_amount, fee_amount,
                        late_penalty_amount, total_amount, paid_total, due_date, status,
                        paid_date
                  FROM  installments
                 WHERE  tenant_id = ? AND loan_id = ?
                 ORDER  BY installment_number
                """;

        var items = jdbcTemplate.query(sql,
                (rs, rn) -> new ScheduleInstallment(
                        rs.getInt("installment_number"),
                        rs.getBigDecimal("principal_amount"),
                        rs.getBigDecimal("profit_amount"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("late_penalty_amount"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getString("status"),
                        rs.getDate("paid_date") != null ? rs.getDate("paid_date").toLocalDate() : null,
                        rs.getBigDecimal("paid_total")
                ),
                tenantId, loanId);

        return ResponseEntity.ok(items);
    }

    // ══════════════════════════════════════════════════════════════
    // EARLY SETTLEMENTS
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "collections.reports", act = "read")
    @GetMapping("/early-settlements")
    @Operation(summary = "Early settlements within a date window")
    public ResponseEntity<List<EarlySettlementItem>> earlySettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var sql = """
                SELECT  loan_id, customer_id, settlement_date,
                        (outstanding_principal + outstanding_profit + outstanding_fees) AS outstanding_total,
                        ibra_amount
                  FROM  settlements
                 WHERE  tenant_id = ?
                   AND  settlement_date IS NOT NULL
                   AND  settlement_date BETWEEN ? AND ?
                 ORDER  BY settlement_date DESC
                """;

        var items = jdbcTemplate.query(sql,
                (rs, rn) -> new EarlySettlementItem(
                        UUID.fromString(rs.getString("loan_id")),
                        rs.getString("customer_id") != null
                                ? UUID.fromString(rs.getString("customer_id")) : null,
                        rs.getDate("settlement_date").toLocalDate(),
                        rs.getBigDecimal("outstanding_total"),
                        rs.getBigDecimal("ibra_amount")
                ),
                tenantId, Date.valueOf(fromDate), Date.valueOf(toDate));

        return ResponseEntity.ok(items);
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private UUID extractTenantId(Jwt jwt) {
        var claim = jwt.getClaimAsString("tenant_id");
        if (claim == null) throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                "No tenant_id claim found in JWT");
        return UUID.fromString(claim);
    }

    private String dpdBucket(int dpd) {
        if (dpd <= 0)  return "CURRENT";
        if (dpd <= 30) return "1-30";
        if (dpd <= 60) return "31-60";
        if (dpd <= 90) return "61-90";
        return "90+";
    }

    // ── Row helper + DTOs ─────────────────────────────────────────

    private record OverdueAggRow(UUID loanId, LocalDate oldestUnpaid,
                                 BigDecimal principalOverdue, BigDecimal profitOverdue,
                                 BigDecimal penaltyAmount, BigDecimal totalOverdue) {}

    public record OverdueInstallmentsResponse(LocalDate asOfDate, int totalCount,
                                              BigDecimal totalOverdueAmount,
                                              List<OverdueItem> items) {}

    public record OverdueItem(UUID loanId, LocalDate oldestUnpaidDate,
                              BigDecimal principalOverdue, BigDecimal profitOverdue,
                              BigDecimal penaltyAmount, BigDecimal totalOverdue,
                              int daysPastDue, String dpdBucket) {}

    public record DueInstallmentsResponse(LocalDate fromDate, LocalDate toDate, int totalCount,
                                          BigDecimal totalDueAmount, List<DueItem> items) {}

    public record DueItem(UUID loanId, int installmentNumber, LocalDate dueDate,
                          BigDecimal principalDue, BigDecimal profitDue, BigDecimal installmentAmount,
                          int daysUntilDue, String status) {}

    public record ScheduleInstallment(int installmentNumber, BigDecimal principalDue,
                                      BigDecimal profitDue, BigDecimal installmentAmount,
                                      BigDecimal penaltyAmount, LocalDate dueDate, String status,
                                      LocalDate paymentDate, BigDecimal amountPaid) {}

    public record EarlySettlementItem(UUID loanId, UUID customerId, LocalDate settlementDate,
                                      BigDecimal outstandingAmount, BigDecimal rebateAmount) {}
}
