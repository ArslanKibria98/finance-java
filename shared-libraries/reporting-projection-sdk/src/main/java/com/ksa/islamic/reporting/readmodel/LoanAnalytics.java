package com.ksa.islamic.reporting.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for loan analytics and trends.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAnalytics {

    private LocalDate period; // Month/Quarter/Year
    private Long loanCount;
    private BigDecimal totalDisbursed;
    private BigDecimal averageLoanSize;
    private Long uniqueCustomers;
    private BigDecimal averageRate;

    // Growth metrics
    private BigDecimal monthOverMonthGrowth;
    private BigDecimal yearOverYearGrowth;

    // Product distribution
    private String topProduct;
    private Long topProductCount;

    // Risk metrics for the period
    private BigDecimal periodParRate;
    private Long newDelinquencies;
    private BigDecimal recoveredAmount;

    /**
     * Calculate disbursement per customer.
     */
    public BigDecimal getDisbursementPerCustomer() {
        if (uniqueCustomers == null || uniqueCustomers == 0) {
            return BigDecimal.ZERO;
        }
        return totalDisbursed.divide(
                BigDecimal.valueOf(uniqueCustomers),
                2,
                java.math.RoundingMode.HALF_UP
        );
    }

    /**
     * Get period label for display.
     */
    public String getPeriodLabel() {
        if (period == null) {
            return "";
        }
        return period.getMonth().name() + " " + period.getYear();
    }
}