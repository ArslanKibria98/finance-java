package com.ksa.islamic.reporting.repository;

import com.ksa.islamic.reporting.readmodel.CustomerPortfolioReadModel;
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
 * Read-only repository for customer analytics and portfolio queries.
 */
@Repository
public interface CustomerReadRepository extends ReadModelRepository<CustomerPortfolioReadModel, String> {

    /**
     * Find customer portfolio by customer ID.
     */
    Optional<CustomerPortfolioReadModel> findByCustomerId(String customerId);

    /**
     * Find top customers by outstanding amount.
     */
    @Query("SELECT c FROM CustomerPortfolioReadModel c ORDER BY c.totalOutstanding DESC")
    Page<CustomerPortfolioReadModel> findTopCustomersByOutstanding(Pageable pageable);

    /**
     * Find customers with overdue loans.
     */
    @Query("SELECT c FROM CustomerPortfolioReadModel c WHERE c.overdueAmount > 0 " +
            "ORDER BY c.overdueAmount DESC")
    List<CustomerPortfolioReadModel> findCustomersWithOverdue();

    /**
     * Find customers by risk category.
     */
    List<CustomerPortfolioReadModel> findByRiskCategory(String riskCategory);

    /**
     * Get customer segment analysis.
     */
    @Query(value = """
            SELECT
                customer_segment,
                COUNT(*) as customerCount,
                AVG(total_loans) as avgLoansPerCustomer,
                SUM(total_outstanding) as totalOutstanding,
                AVG(total_outstanding) as avgOutstanding,
                SUM(overdue_amount) as totalOverdue
            FROM customer_portfolio_read_model
            WHERE tenant_id = :tenantId
            GROUP BY customer_segment
            ORDER BY totalOutstanding DESC
            """, nativeQuery = true)
    List<Object[]> getCustomerSegmentAnalysis(@Param("tenantId") String tenantId);

    /**
     * Find customers with multiple active loans.
     */
    @Query("SELECT c FROM CustomerPortfolioReadModel c WHERE c.activeLoans > :loanCount")
    List<CustomerPortfolioReadModel> findCustomersWithMultipleLoans(@Param("loanCount") int loanCount);

    /**
     * Get customer acquisition trends.
     */
    @Query(value = """
            SELECT
                DATE_TRUNC('month', first_loan_date) as period,
                COUNT(*) as newCustomers,
                AVG(first_loan_amount) as avgFirstLoanAmount
            FROM customer_portfolio_read_model
            WHERE first_loan_date BETWEEN :startDate AND :endDate
                AND tenant_id = :tenantId
            GROUP BY DATE_TRUNC('month', first_loan_date)
            ORDER BY period DESC
            """, nativeQuery = true)
    List<Object[]> getCustomerAcquisitionTrends(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find VIP customers (high value, low risk).
     */
    @Query("SELECT c FROM CustomerPortfolioReadModel c WHERE " +
            "c.totalOutstanding > :minOutstanding AND c.riskCategory = 'LOW' " +
            "ORDER BY c.totalOutstanding DESC")
    List<CustomerPortfolioReadModel> findVipCustomers(@Param("minOutstanding") BigDecimal minOutstanding);

    /**
     * Search customers by name or national ID.
     */
    @Query("SELECT c FROM CustomerPortfolioReadModel c WHERE " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.nationalId LIKE CONCAT('%', :searchTerm, '%')")
    Page<CustomerPortfolioReadModel> searchCustomers(
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    /**
     * Get customer lifetime value analysis.
     */
    @Query(value = """
            SELECT
                customer_id,
                customer_name,
                total_loans as lifetimeLoans,
                total_paid_interest as lifetimeRevenue,
                total_principal_paid as lifetimePrincipalPaid,
                (total_paid_interest / NULLIF(total_loans, 0)) as avgInterestPerLoan,
                EXTRACT(YEAR FROM AGE(CURRENT_DATE, first_loan_date)) * 12 +
                EXTRACT(MONTH FROM AGE(CURRENT_DATE, first_loan_date)) as tenureMonths
            FROM customer_portfolio_read_model
            WHERE tenant_id = :tenantId
            ORDER BY lifetimeRevenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getCustomerLifetimeValue(
            @Param("tenantId") String tenantId,
            @Param("limit") int limit);
}