package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single installment in the repayment schedule.
 */
@Data
@Builder
public class Installment {

    private int installmentNumber;
    private LocalDate dueDate;
    private BigDecimal totalAmount;

    // Amount Breakdown
    private BigDecimal principalAmount;
    private BigDecimal profitAmount;
    private BigDecimal feeAmount;

    // Payment Status
    private InstallmentStatus status;
    private LocalDate paidDate;
    private BigDecimal paidAmount;

    // Balance after installment
    private BigDecimal principalBalance;
    private BigDecimal profitBalance;

    // Delinquency
    private int daysOverdue;
    private BigDecimal penaltyAmount;
    private BigDecimal charityAmount; // Islamic finance late fees

    // Additional Info
    private String paymentReference;
    private boolean isGracePeriod;

    public enum InstallmentStatus {
        PENDING,
        PAID,
        PARTIALLY_PAID,
        OVERDUE,
        WAIVED,
        RESTRUCTURED
    }

    public boolean isPaid() {
        return status == InstallmentStatus.PAID;
    }

    public boolean isOverdue() {
        return status == InstallmentStatus.OVERDUE;
    }

    public BigDecimal getRemainingAmount() {
        if (paidAmount == null) {
            return totalAmount;
        }
        return totalAmount.subtract(paidAmount);
    }
}