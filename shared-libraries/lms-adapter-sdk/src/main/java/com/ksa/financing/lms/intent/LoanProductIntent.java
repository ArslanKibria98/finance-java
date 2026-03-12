package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Loan Product Intent - Domain-focused request for creating a loan product in the CBS.
 * Contains what the domain wants, not how the CBS should do it.
 */
@Data
@Builder
public class LoanProductIntent {

    private String productCode;
    private String name;
    private String shortName;
    private String description;
    private String currency;

    // Amount range
    private BigDecimal minPrincipal;
    private BigDecimal maxPrincipal;
    private BigDecimal defaultPrincipal;

    // Tenure range
    private int minTenureMonths;
    private int maxTenureMonths;
    private int defaultTenureMonths;

    // Profit (Islamic finance — mapped to interest in Fineract)
    private BigDecimal annualProfitRate;
    private String rateType; // REDUCING_BALANCE, FLAT

    // Repayment
    private String repaymentFrequency; // MONTHLY, QUARTERLY
    private int gracePeriodDays;
    private boolean earlySettlementAllowed;

    // Islamic finance specific
    private String shariaStructure; // MURABAHA, TAWARRUQ, IJARA, etc.

    // Metadata
    private String requestId; // For idempotency
    private String tenantId;
    private String externalId; // Our product UUID
}
