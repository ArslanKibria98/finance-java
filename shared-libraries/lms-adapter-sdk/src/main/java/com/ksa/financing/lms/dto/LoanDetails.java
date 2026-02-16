package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Complete loan details retrieved from the LMS.
 */
@Data
@Builder
public class LoanDetails {

    // Identification
    private LoanAccountId loanAccountId;
    private String accountNumber;
    private String customerId;
    private String customerName;

    // Loan Terms
    private String productCode;
    private String productName;
    private BigDecimal principalAmount;
    private BigDecimal profitAmount;
    private BigDecimal totalAmount;
    private int tenureInMonths;
    private String repaymentFrequency;

    // Islamic Finance Details
    private String shariaStructure;
    private BigDecimal commodityCost;
    private BigDecimal commoditySalePrice;
    private String commodityReference;
    private boolean shariaCompliant;

    // Status
    private LoanStatus status;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;

    // Balances
    private BigDecimal disbursedAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingProfit;
    private BigDecimal totalOutstanding;
    private BigDecimal prepaymentAmount;

    // Payment Information
    private BigDecimal installmentAmount;
    private LocalDate lastPaymentDate;
    private BigDecimal lastPaymentAmount;
    private LocalDate nextDueDate;
    private int installmentsPaid;
    private int installmentsRemaining;

    // Delinquency
    private int daysInArrears;
    private BigDecimal arrearsAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal charityAmount;

    // Additional Info
    private String branchCode;
    private String officerId;
    private String collateralDetails;
    private LocalDateTime lastModifiedDate;

    public boolean isActive() {
        return status == LoanStatus.ACTIVE ||
               status == LoanStatus.IN_ARREARS;
    }

    public boolean isDelinquent() {
        return daysInArrears > 0;
    }

    public BigDecimal getEarlySettlementAmount() {
        // Base settlement amount before Ibra calculation
        return outstandingPrincipal.add(outstandingProfit);
    }
}