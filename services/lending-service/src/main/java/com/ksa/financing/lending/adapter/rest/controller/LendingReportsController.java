package com.ksa.financing.lending.adapter.rest.controller;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregation endpoint consumed by ledger-service's LoanBalanceOutstandingReportService
 * to produce the Loan Balance & Outstanding Report.
 */
@RestController
@RequestMapping("/api/v1/internal/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lending Reports", description = "Internal loan aggregation for outstanding balance reports")
public class LendingReportsController {

    private final JdbcTemplate jdbcTemplate;

    @SecuredEndpoint(obj = "lending.reports", act = "read")
    @GetMapping("/outstanding-balances")
    @Operation(summary = "Outstanding balances for all active loans as of a date")
    public ResponseEntity<OutstandingBalancesResponse> outstandingBalances(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) String productCode,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var reportDate = asOfDate != null ? asOfDate : LocalDate.now();

        var sql = """
                SELECT  id, loan_number, customer_id, product_code,
                        principal_amount, profit_amount,
                        outstanding_principal, outstanding_profit, outstanding_fees,
                        (principal_amount + profit_amount
                         - outstanding_principal - outstanding_profit - outstanding_fees) AS total_paid,
                        status
                  FROM  loans
                 WHERE  tenant_id = ?
                   AND  deleted_at IS NULL
                   AND  status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')
                   AND  (CAST(? AS text) IS NULL OR product_code = CAST(? AS text))
                 ORDER  BY loan_number
                """;

        var rows = jdbcTemplate.query(sql,
                (rs, rn) -> new Row(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("loan_number"),
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("product_code"),
                        rs.getBigDecimal("principal_amount"),
                        rs.getBigDecimal("outstanding_principal"),
                        rs.getBigDecimal("outstanding_profit"),
                        rs.getBigDecimal("outstanding_fees"),
                        rs.getBigDecimal("total_paid"),
                        rs.getString("status")
                ),
                tenantId, productCode, productCode);

        var items = new ArrayList<OutstandingItem>();
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (var r : rows) {
            items.add(new OutstandingItem(
                    r.loanId(), r.loanNumber(), r.customerId(), r.productCode(),
                    r.disbursedAmount(), r.totalPaid(),
                    r.outstandingPrincipal(), r.outstandingProfit(), r.outstandingFees(),
                    r.status()
            ));
            totalPrincipal = totalPrincipal.add(r.outstandingPrincipal());
            totalProfit = totalProfit.add(r.outstandingProfit());
            totalFees = totalFees.add(r.outstandingFees());
        }

        log.info("Outstanding balances report: tenant={} asOf={} count={} principal={}",
                tenantId, reportDate, items.size(), totalPrincipal);

        return ResponseEntity.ok(new OutstandingBalancesResponse(
                reportDate, items.size(), totalPrincipal, totalProfit, totalFees, items));
    }

    @SecuredEndpoint(obj = "lending.reports", act = "read")
    @GetMapping("/loan-lookup")
    @Operation(summary = "Fetch basic loan info by loan IDs (used to enrich reports with loan_number + product_code)")
    public ResponseEntity<List<LoanLookupItem>> loanLookup(
            @RequestParam List<UUID> loanIds,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        if (loanIds == null || loanIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        var placeholders = String.join(",", loanIds.stream().map(i -> "?").toList());
        var sql = "SELECT id, loan_number, customer_id, product_code, principal_amount, " +
                 "profit_amount, tenure_months, status FROM loans " +
                 "WHERE tenant_id = ? AND id IN (" + placeholders + ")";

        var args = new java.util.ArrayList<Object>();
        args.add(tenantId);
        args.addAll(loanIds);

        var items = jdbcTemplate.query(sql,
                (rs, rn) -> new LoanLookupItem(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("loan_number"),
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("product_code"),
                        rs.getBigDecimal("principal_amount"),
                        rs.getBigDecimal("profit_amount"),
                        rs.getInt("tenure_months"),
                        rs.getString("status")
                ),
                args.toArray());

        return ResponseEntity.ok(items);
    }

    @SecuredEndpoint(obj = "lending.reports", act = "read")
    @GetMapping("/disbursed-loans")
    @Operation(summary = "Fetch disbursed loans in date range for reporting")
    public ResponseEntity<List<DisbursedLoanItem>> disbursedLoans(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String productCode,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var sql = """
                SELECT l.id,
                       la.application_number,
                       l.loan_number,
                       l.customer_id,
                       COALESCE(la.national_id, '') AS national_id,
                       l.product_code,
                       COALESCE(la.product_name, l.product_code, '') AS product_name,
                       l.disbursement_date,
                       l.principal_amount,
                       l.tenure_months,
                       l.status,
                       COALESCE(NULLIF(la.disbursement_bank_name, ''),
                                NULLIF(la.disbursement_bank_code, ''),
                                'UNKNOWN') AS branch_or_channel
                  FROM loans l
                  LEFT JOIN loan_applications la
                    ON la.id = l.application_id
                   AND la.tenant_id = l.tenant_id
                 WHERE l.tenant_id = ?
                   AND l.deleted_at IS NULL
                   AND l.disbursement_date >= ?
                   AND l.disbursement_date <= ?
                   AND (
                        CAST(? AS text) IS NULL
                        OR REGEXP_REPLACE(UPPER(COALESCE(l.product_code, '')), '[^A-Z0-9]', '', 'g')
                           LIKE '%' || REGEXP_REPLACE(UPPER(CAST(? AS text)), '[^A-Z0-9]', '', 'g') || '%'
                        OR REGEXP_REPLACE(UPPER(COALESCE(la.product_name, '')), '[^A-Z0-9]', '', 'g')
                           LIKE '%' || REGEXP_REPLACE(UPPER(CAST(? AS text)), '[^A-Z0-9]', '', 'g') || '%'
                   )
                 ORDER BY l.disbursement_date DESC, l.loan_number
                """;

        var rows = jdbcTemplate.query(
                sql,
                (rs, rn) -> new DisbursedLoanItem(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("application_number"),
                        rs.getString("loan_number"),
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("national_id"),
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getObject("disbursement_date", LocalDate.class),
                        rs.getBigDecimal("principal_amount"),
                        rs.getInt("tenure_months"),
                        rs.getString("status"),
                        rs.getString("branch_or_channel")
                ),
                tenantId, fromDate, toDate, productCode, productCode, productCode
        );

        return ResponseEntity.ok(rows);
    }

    private UUID extractTenantId(Jwt jwt) {
        var claim = jwt.getClaimAsString("tenant_id");
        if (claim == null) throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                "No tenant_id claim found in JWT");
        return UUID.fromString(claim);
    }

    // ── Row helper + DTOs ─────────────────────────────────────────

    private record Row(UUID loanId, String loanNumber, UUID customerId, String productCode,
                       BigDecimal disbursedAmount,
                       BigDecimal outstandingPrincipal, BigDecimal outstandingProfit,
                       BigDecimal outstandingFees, BigDecimal totalPaid, String status) {}

    public record OutstandingBalancesResponse(LocalDate asOfDate, int totalCount,
                                              BigDecimal totalPrincipalOutstanding,
                                              BigDecimal totalProfitOutstanding,
                                              BigDecimal totalFeesOutstanding,
                                              List<OutstandingItem> items) {}

    public record OutstandingItem(UUID loanId, String loanNumber, UUID customerId, String productCode,
                                  BigDecimal disbursedAmount, BigDecimal totalPaid,
                                  BigDecimal outstandingPrincipal, BigDecimal outstandingProfit,
                                  BigDecimal outstandingFees, String status) {}

    public record LoanLookupItem(UUID loanId, String loanNumber, UUID customerId, String productCode,
                                 BigDecimal principalAmount, BigDecimal profitAmount,
                                 int tenureMonths, String status) {}

    public record DisbursedLoanItem(UUID loanId,
                                    String applicationNumber,
                                    String loanNumber,
                                    UUID customerId,
                                    String nationalId,
                                    String productCode,
                                    String productName,
                                    LocalDate disbursementDate,
                                    BigDecimal disbursedAmount,
                                    Integer tenureMonths,
                                    String status,
                                    String branchOrChannel) {}
}
