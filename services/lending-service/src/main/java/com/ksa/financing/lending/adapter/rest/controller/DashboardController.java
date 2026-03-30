package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin dashboard statistics")
public class DashboardController {

    private final EntityManager entityManager;

    @SecuredEndpoint(obj = "dashboard", act = "read")
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics (cards + collection + finance stats + recent applications)")
    public ResponseEntity<DashboardResponse> getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        var cards = getCardStats(tenantId, from, to);
        var collection = getCollectionStats(tenantId);
        var financeStats = getFinanceStatistics(tenantId, from, to);
        var recentApps = getRecentApplications(tenantId);

        return ResponseEntity.ok(new DashboardResponse(cards, collection, financeStats, recentApps));
    }

    @SuppressWarnings("unchecked")
    private CardStats getCardStats(UUID tenantId, LocalDate from, LocalDate to) {
        var fromTs = from.atStartOfDay();
        var toTs = to.plusDays(1).atStartOfDay();

        Query q = entityManager.createNativeQuery("""
            SELECT
                COUNT(*) FILTER (WHERE status NOT IN ('CANCELLED','EXPIRED')) AS total_applied,
                COUNT(*) FILTER (WHERE status = 'APPROVED') AS total_completed,
                COUNT(*) FILTER (WHERE status NOT IN ('APPROVED','REJECTED','CANCELLED','EXPIRED')) AS total_in_progress,
                COUNT(*) FILTER (WHERE status = 'APPROVED') AS total_approved,
                COUNT(*) FILTER (WHERE status = 'REJECTED') AS total_rejected,
                COUNT(*) FILTER (WHERE status = 'APPROVED' AND updated_at >= :todayStart AND updated_at < :todayEnd) AS todays_completed,
                COALESCE(SUM(accepted_amount) FILTER (WHERE status = 'APPROVED'), 0) AS total_disbursed_amount,
                COUNT(DISTINCT customer_id) AS total_customers
            FROM loan_applications
            WHERE tenant_id = :tenantId
              AND created_at >= :fromTs AND created_at < :toTs
        """);
        q.setParameter("tenantId", tenantId);
        q.setParameter("fromTs", fromTs);
        q.setParameter("toTs", toTs);
        q.setParameter("todayStart", LocalDate.now().atStartOfDay());
        q.setParameter("todayEnd", LocalDate.now().plusDays(1).atStartOfDay());

        Object[] row = (Object[]) q.getSingleResult();

        return new CardStats(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                toBigDecimal(row[6]),
                ((Number) row[7]).longValue()
        );
    }

    @SuppressWarnings("unchecked")
    private CollectionStats getCollectionStats(UUID tenantId) {
        Query q = entityManager.createNativeQuery("""
            SELECT
                COUNT(*) AS total_active_loans,
                COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active_loans,
                COUNT(*) FILTER (WHERE status = 'DELINQUENT') AS delinquent_loans,
                COUNT(*) FILTER (WHERE status = 'DEFAULT') AS defaulted_loans,
                COUNT(*) FILTER (WHERE status = 'RESTRUCTURED') AS restructured_loans,
                COUNT(*) FILTER (WHERE status = 'SETTLED') AS settled_loans,
                COUNT(*) FILTER (WHERE status = 'WRITTEN_OFF') AS written_off_loans,
                COALESCE(SUM(principal_amount), 0) AS total_principal,
                COALESCE(SUM(total_outstanding), 0) AS total_outstanding,
                COALESCE(SUM(total_outstanding) FILTER (WHERE status IN ('DELINQUENT','DEFAULT')), 0) AS total_overdue_amount,
                COALESCE(SUM(principal_amount) FILTER (WHERE status = 'ACTIVE'), 0) AS active_portfolio,
                COALESCE(AVG(current_dpd) FILTER (WHERE current_dpd > 0), 0) AS avg_dpd,
                COALESCE(MAX(current_dpd), 0) AS max_dpd,
                COUNT(*) FILTER (WHERE current_dpd BETWEEN 1 AND 30) AS dpd_1_30,
                COUNT(*) FILTER (WHERE current_dpd BETWEEN 31 AND 60) AS dpd_31_60,
                COUNT(*) FILTER (WHERE current_dpd BETWEEN 61 AND 90) AS dpd_61_90,
                COUNT(*) FILTER (WHERE current_dpd > 90) AS dpd_over_90,
                COUNT(DISTINCT customer_id) AS total_borrowers
            FROM loans
            WHERE tenant_id = :tenantId
        """);
        q.setParameter("tenantId", tenantId);

        Object[] row = (Object[]) q.getSingleResult();

        return new CollectionStats(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                toBigDecimal(row[7]),
                toBigDecimal(row[8]),
                toBigDecimal(row[9]),
                toBigDecimal(row[10]),
                ((Number) row[11]).intValue(),
                ((Number) row[12]).intValue(),
                new DpdBuckets(
                        ((Number) row[13]).longValue(),
                        ((Number) row[14]).longValue(),
                        ((Number) row[15]).longValue(),
                        ((Number) row[16]).longValue()
                ),
                ((Number) row[17]).longValue()
        );
    }

    @SuppressWarnings("unchecked")
    private List<FinanceStatEntry> getFinanceStatistics(UUID tenantId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery("""
            SELECT
                CAST(d AS date) AS stat_date,
                COUNT(*) FILTER (WHERE la.status IS NOT NULL AND la.status NOT IN ('CANCELLED','EXPIRED')) AS applied,
                COUNT(*) FILTER (WHERE la.status = 'APPROVED') AS approved,
                COUNT(*) FILTER (WHERE la.status = 'REJECTED') AS rejected,
                COUNT(*) FILTER (WHERE la.status = 'APPROVED') AS disbursed
            FROM generate_series(CAST(:from AS date), CAST(:to AS date), CAST('1 day' AS interval)) d
            LEFT JOIN loan_applications la
                ON la.tenant_id = :tenantId
                AND la.created_at >= d AND la.created_at < d + CAST('1 day' AS interval)
            GROUP BY CAST(d AS date)
            ORDER BY CAST(d AS date)
        """);
        q.setParameter("tenantId", tenantId);
        q.setParameter("from", from);
        q.setParameter("to", to);

        List<Object[]> rows = q.getResultList();
        List<FinanceStatEntry> stats = new ArrayList<>();
        for (Object[] row : rows) {
            stats.add(new FinanceStatEntry(
                    row[0].toString(),
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue()
            ));
        }
        return stats;
    }

    @SuppressWarnings("unchecked")
    private List<RecentApplication> getRecentApplications(UUID tenantId) {
        Query q = entityManager.createNativeQuery("""
            SELECT id, application_number, customer_id, national_id, product_name,
                   requested_amount, status, created_at
            FROM loan_applications
            WHERE tenant_id = :tenantId
            ORDER BY created_at DESC
            LIMIT 10
        """);
        q.setParameter("tenantId", tenantId);

        List<Object[]> rows = q.getResultList();
        List<RecentApplication> apps = new ArrayList<>();
        for (Object[] row : rows) {
            apps.add(new RecentApplication(
                    row[0].toString(),
                    (String) row[1],
                    row[2] != null ? row[2].toString() : null,
                    (String) row[3],
                    (String) row[4],
                    row[5] instanceof BigDecimal ? (BigDecimal) row[5] : (row[5] != null ? BigDecimal.valueOf(((Number) row[5]).doubleValue()) : null),
                    (String) row[6],
                    row[7] != null ? row[7].toString() : null
            ));
        }
        return apps;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    // ── Response Records ──

    public record DashboardResponse(
            CardStats cards,
            CollectionStats collection,
            List<FinanceStatEntry> financeStatistics,
            List<RecentApplication> recentApplications
    ) {}

    public record CardStats(
            long totalAppliedApplications,
            long totalCompletedApplications,
            long totalInProgressApplications,
            long totalApprovedApplications,
            long totalRejectedApplications,
            long todaysCompletedApplications,
            BigDecimal totalDisbursedAmount,
            long totalCustomers
    ) {}

    public record CollectionStats(
            long totalLoans,
            long activeLoans,
            long delinquentLoans,
            long defaultedLoans,
            long restructuredLoans,
            long settledLoans,
            long writtenOffLoans,
            BigDecimal totalPrincipal,
            BigDecimal totalOutstanding,
            BigDecimal totalOverdueAmount,
            BigDecimal activePortfolio,
            int avgDpd,
            int maxDpd,
            DpdBuckets dpdBuckets,
            long totalBorrowers
    ) {}

    public record DpdBuckets(
            long dpd1to30,
            long dpd31to60,
            long dpd61to90,
            long dpdOver90
    ) {}

    public record FinanceStatEntry(
            String date,
            long applied,
            long approved,
            long rejected,
            long disbursed
    ) {}

    public record RecentApplication(
            String applicationId,
            String applicationNumber,
            String customerId,
            String nationalId,
            String productName,
            BigDecimal requestedAmount,
            String status,
            String createdAt
    ) {}

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
