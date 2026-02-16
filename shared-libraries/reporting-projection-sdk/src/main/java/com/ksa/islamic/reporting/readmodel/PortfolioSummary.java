package com.ksa.islamic.reporting.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO for portfolio summary statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummary {

    private Long totalLoans;
    private Long activeLoans;
    private BigDecimal totalPrincipal;
    private BigDecimal totalOutstanding;
    private BigDecimal portfolioAtRisk; // Outstanding amount of overdue loans
    private BigDecimal averageInterestRate;
    private Double averageDaysOverdue;
    private Long loansOver30Days;
    private Long loansOver60Days;
    private Long loansOver90Days;

    // Calculated metrics

    /**
     * Calculate Portfolio at Risk (PAR) percentage.
     */
    public BigDecimal getParPercentage() {
        if (totalOutstanding == null || totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return portfolioAtRisk
                .divide(totalOutstanding, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate Non-Performing Loans (NPL) ratio.
     */
    public BigDecimal getNplRatio() {
        if (totalLoans == null || totalLoans == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(loansOver90Days)
                .divide(BigDecimal.valueOf(totalLoans), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate collection rate.
     */
    public BigDecimal getCollectionRate() {
        if (totalPrincipal == null || totalPrincipal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal collected = totalPrincipal.subtract(totalOutstanding);
        return collected
                .divide(totalPrincipal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get portfolio health status based on NPL ratio.
     */
    public String getHealthStatus() {
        BigDecimal npl = getNplRatio();
        if (npl.compareTo(BigDecimal.valueOf(2)) < 0) {
            return "EXCELLENT";
        } else if (npl.compareTo(BigDecimal.valueOf(5)) < 0) {
            return "GOOD";
        } else if (npl.compareTo(BigDecimal.valueOf(10)) < 0) {
            return "MODERATE";
        } else {
            return "CRITICAL";
        }
    }
}