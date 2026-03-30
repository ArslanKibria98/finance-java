package com.ksa.financing.lending.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Input port for BRD Steps 4-10: Pre-qualification / Check Eligibility.
 * Stateless: no DB writes, no workflow. Just affordability + finance calculation.
 */
public interface CheckEligibilityUseCase {

    EligibilityCheckResult checkEligibility(CheckEligibilityCommand command);

    record CheckEligibilityCommand(
            UUID tenantId,
            BigDecimal amount,
            int tenureMonths,
            BigDecimal salary,
            BigDecimal liabilities,
            int adultDependents,
            int childDependents,
            BigDecimal foodGroceries,
            BigDecimal utilities,
            BigDecimal healthcare,
            BigDecimal communication,
            BigDecimal housingRent,
            BigDecimal clothingEssentials,
            BigDecimal education,
            BigDecimal transportation,
            String productId
    ) {}

    record EligibilityCheckResult(
            boolean eligible,
            BigDecimal monthlyInstallment,
            int tenure,
            int numInstallments,
            BigDecimal totalPayable,
            BigDecimal costOfTerm,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            BigDecimal disposableIncome,
            BigDecimal maxEligibleAmount,
            LocalDate firstInstallmentDueDate,
            String reason
    ) {}
}
