package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loan Intent - Domain-focused request for creating a loan.
 * Contains what the domain wants, not how the CBS should do it.
 */
@Data
@Builder
public class LoanIntent {

    // Customer Information
    private String customerId;
    private String customerName;
    private String nationalId;

    // Loan Details
    private String productCode;
    private BigDecimal principalAmount;
    private BigDecimal profitAmount; // Islamic finance profit (not interest)
    private int tenureInMonths;
    private LocalDate expectedDisbursementDate;

    // Islamic Finance Specific
    private String shariaStructure; // TAWARRUQ, MURABAHA, etc.
    private String commodityId; // For Tawarruq transactions
    private BigDecimal commodityCost;
    private BigDecimal commoditySalePrice;

    // Repayment Details
    private String repaymentFrequency; // MONTHLY, QUARTERLY
    private BigDecimal installmentAmount;
    private LocalDate firstInstallmentDate;

    // Additional Information
    private String purpose; // Purpose of financing
    private String collateralDetails;
    private String branchCode;
    private String officerId;

    // Metadata
    private String requestId; // For idempotency
    private String sourceSystem;
    private String tenantId;
}