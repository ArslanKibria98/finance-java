package com.ksa.islamic.reporting.repository;

import com.ksa.islamic.reporting.readmodel.PaymentHistoryReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only repository for payment history and analytics.
 */
@Repository
public interface PaymentReadRepository extends ReadModelRepository<PaymentHistoryReadModel, String> {

    /**
     * Find payments by loan ID.
     */
    List<PaymentHistoryReadModel> findByLoanIdOrderByPaymentDateDesc(String loanId);

    /**
     * Find payments by customer ID.
     */
    List<PaymentHistoryReadModel> findByCustomerIdOrderByPaymentDateDesc(String customerId);

    /**
     * Find payments within a date range.
     */
    @Query("SELECT p FROM PaymentHistoryReadModel p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    Page<PaymentHistoryReadModel> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Get payment collection summary by period.
     */
    @Query(value = """
            SELECT
                DATE_TRUNC(:period, payment_date) as collectionPeriod,
                COUNT(*) as paymentCount,
                SUM(payment_amount) as totalCollected,
                SUM(principal_portion) as principalCollected,
                SUM(interest_portion) as interestCollected,
                SUM(late_fee) as lateFeeCollected,
                AVG(payment_amount) as avgPaymentAmount,
                COUNT(DISTINCT customer_id) as uniqueCustomers
            FROM payment_history_read_model
            WHERE payment_date BETWEEN :startDate AND :endDate
                AND tenant_id = :tenantId
            GROUP BY collectionPeriod
            ORDER BY collectionPeriod DESC
            """, nativeQuery = true)
    List<Object[]> getCollectionSummaryByPeriod(
            @Param("tenantId") String tenantId,
            @Param("period") String period, // 'day', 'week', 'month', 'quarter'
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find late payments.
     */
    @Query("SELECT p FROM PaymentHistoryReadModel p WHERE p.daysLate > 0 " +
            "ORDER BY p.daysLate DESC, p.paymentDate DESC")
    Page<PaymentHistoryReadModel> findLatePayments(Pageable pageable);

    /**
     * Get payment channel distribution.
     */
    @Query(value = """
            SELECT
                payment_channel,
                COUNT(*) as transactionCount,
                SUM(payment_amount) as totalAmount,
                AVG(payment_amount) as avgAmount
            FROM payment_history_read_model
            WHERE payment_date BETWEEN :startDate AND :endDate
                AND tenant_id = :tenantId
            GROUP BY payment_channel
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<Object[]> getPaymentChannelDistribution(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find bounced or failed payments.
     */
    @Query("SELECT p FROM PaymentHistoryReadModel p WHERE p.status IN ('BOUNCED', 'FAILED', 'REVERSED')")
    List<PaymentHistoryReadModel> findFailedPayments();

    /**
     * Get collection efficiency metrics.
     */
    @Query(value = """
            SELECT
                DATE_TRUNC('month', due_date) as month,
                SUM(scheduled_amount) as totalDue,
                SUM(CASE WHEN payment_date <= due_date THEN payment_amount ELSE 0 END) as onTimeCollection,
                SUM(CASE WHEN payment_date > due_date THEN payment_amount ELSE 0 END) as lateCollection,
                COUNT(DISTINCT CASE WHEN payment_date IS NOT NULL THEN loan_id END) as loansWithPayment,
                COUNT(DISTINCT loan_id) as totalLoans
            FROM payment_history_read_model
            WHERE due_date BETWEEN :startDate AND :endDate
                AND tenant_id = :tenantId
            GROUP BY month
            ORDER BY month DESC
            """, nativeQuery = true)
    List<Object[]> getCollectionEfficiency(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find prepayments.
     */
    @Query("SELECT p FROM PaymentHistoryReadModel p WHERE p.isPrepayment = true " +
            "ORDER BY p.paymentDate DESC")
    List<PaymentHistoryReadModel> findPrepayments();

    /**
     * Get daily collection trends.
     */
    @Query(value = """
            SELECT
                payment_date,
                COUNT(*) as transactionCount,
                SUM(payment_amount) as dailyTotal,
                MAX(payment_amount) as maxPayment,
                MIN(payment_amount) as minPayment
            FROM payment_history_read_model
            WHERE payment_date >= :fromDate
                AND tenant_id = :tenantId
            GROUP BY payment_date
            ORDER BY payment_date DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getDailyCollectionTrends(
            @Param("tenantId") String tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("limit") int limit);

    /**
     * Find payments requiring reconciliation.
     */
    @Query("SELECT p FROM PaymentHistoryReadModel p WHERE p.reconciliationStatus = 'PENDING'")
    List<PaymentHistoryReadModel> findPaymentsForReconciliation();
}