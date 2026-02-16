package com.ksa.islamic.reporting.repository;

import com.ksa.islamic.reporting.readmodel.LoanSummaryReadModel;
import com.ksa.islamic.reporting.readmodel.PortfolioSummary;
import com.ksa.islamic.reporting.readmodel.LoanAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read-only repository for loan portfolio queries.
 * Optimized for fast read operations with denormalized data.
 */
@Repository
public interface LoanReadRepository extends ReadModelRepository<LoanSummaryReadModel, String> {

    /**
     * Find active loans by customer ID.
     */
    @Query("SELECT l FROM LoanSummaryReadModel l WHERE l.customerId = :customerId " +
            "AND l.status IN ('ACTIVE', 'DISBURSED') ORDER BY l.disbursementDate DESC")
    List<LoanSummaryReadModel> findActiveLoansByCustomer(@Param("customerId") String customerId);

    /**
     * Find overdue loans.
     */
    @Query("SELECT l FROM LoanSummaryReadModel l WHERE l.daysOverdue > 0 " +
            "ORDER BY l.daysOverdue DESC, l.outstandingPrincipal DESC")
    Page<LoanSummaryReadModel> findOverdueLoans(Pageable pageable);

    /**
     * Find loans by status.
     */
    List<LoanSummaryReadModel> findByStatus(String status);

    /**
     * Find loans by product type.
     */
    List<LoanSummaryReadModel> findByProductType(String productType);

    /**
     * Get portfolio summary statistics.
     */
    @Query(value = """
            SELECT
                COUNT(*) as totalLoans,
                COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as activeLoans,
                SUM(principal_amount) as totalPrincipal,
                SUM(outstanding_principal) as totalOutstanding,
                SUM(CASE WHEN days_overdue > 0 THEN outstanding_principal ELSE 0 END) as portfolioAtRisk,
                AVG(interest_rate) as averageInterestRate,
                AVG(days_overdue) as averageDaysOverdue,
                COUNT(CASE WHEN days_overdue > 30 THEN 1 END) as loansOver30Days,
                COUNT(CASE WHEN days_overdue > 60 THEN 1 END) as loansOver60Days,
                COUNT(CASE WHEN days_overdue > 90 THEN 1 END) as loansOver90Days
            FROM loan_summary_read_model
            WHERE tenant_id = :tenantId
            """, nativeQuery = true)
    PortfolioSummary getPortfolioSummary(@Param("tenantId") String tenantId);

    /**
     * Get loan analytics for a specific period.
     */
    @Query(value = """
            SELECT
                DATE_TRUNC('month', disbursement_date) as period,
                COUNT(*) as loanCount,
                SUM(principal_amount) as totalDisbursed,
                AVG(principal_amount) as averageLoanSize,
                COUNT(DISTINCT customer_id) as uniqueCustomers,
                AVG(interest_rate) as averageRate
            FROM loan_summary_read_model
            WHERE disbursement_date BETWEEN :startDate AND :endDate
                AND tenant_id = :tenantId
            GROUP BY DATE_TRUNC('month', disbursement_date)
            ORDER BY period DESC
            """, nativeQuery = true)
    List<LoanAnalytics> getLoanAnalytics(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find loans by maturity date range.
     */
    @Query("SELECT l FROM LoanSummaryReadModel l WHERE l.maturityDate BETWEEN :startDate AND :endDate")
    List<LoanSummaryReadModel> findByMaturityDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find high-value loans above a threshold.
     */
    @Query("SELECT l FROM LoanSummaryReadModel l WHERE l.principalAmount > :threshold " +
            "ORDER BY l.principalAmount DESC")
    Page<LoanSummaryReadModel> findHighValueLoans(
            @Param("threshold") BigDecimal threshold,
            Pageable pageable);

    /**
     * Search loans by customer name (denormalized field).
     */
    @Query("SELECT l FROM LoanSummaryReadModel l WHERE LOWER(l.customerName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<LoanSummaryReadModel> searchByCustomerName(
            @Param("name") String name,
            Pageable pageable);

    /**
     * Get delinquency buckets for reporting.
     */
    @Query(value = """
            SELECT
                CASE
                    WHEN days_overdue = 0 THEN 'Current'
                    WHEN days_overdue BETWEEN 1 AND 30 THEN '1-30 Days'
                    WHEN days_overdue BETWEEN 31 AND 60 THEN '31-60 Days'
                    WHEN days_overdue BETWEEN 61 AND 90 THEN '61-90 Days'
                    WHEN days_overdue > 90 THEN '90+ Days'
                END as bucket,
                COUNT(*) as loanCount,
                SUM(outstanding_principal) as totalOutstanding
            FROM loan_summary_read_model
            WHERE tenant_id = :tenantId
            GROUP BY bucket
            ORDER BY
                CASE bucket
                    WHEN 'Current' THEN 0
                    WHEN '1-30 Days' THEN 1
                    WHEN '31-60 Days' THEN 2
                    WHEN '61-90 Days' THEN 3
                    WHEN '90+ Days' THEN 4
                END
            """, nativeQuery = true)
    List<Object[]> getDelinquencyBuckets(@Param("tenantId") String tenantId);

    /**
     * Find loans requiring collection action.
     */
    @Query("SELECT l FROM LoanSummaryReadModel l WHERE l.daysOverdue > :daysThreshold " +
            "AND l.lastPaymentDate < :paymentDateThreshold " +
            "ORDER BY l.outstandingPrincipal DESC")
    List<LoanSummaryReadModel> findLoansForCollection(
            @Param("daysThreshold") int daysThreshold,
            @Param("paymentDateThreshold") LocalDate paymentDateThreshold);
}