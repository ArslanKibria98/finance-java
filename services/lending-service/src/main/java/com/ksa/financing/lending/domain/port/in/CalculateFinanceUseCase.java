package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.lending.domain.service.FinanceCalculationResult;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Input port for BRD UC#01 — Finance Calculator.
 * Stateless calculation: no DB writes, no workflow.
 */
public interface CalculateFinanceUseCase {

    FinanceCalculationResult calculate(CalculateFinanceCommand command);

    record CalculateFinanceCommand(
            UUID tenantId,
            BigDecimal amount,
            int tenureMonths,
            BigDecimal totalIncome,
            String productId
    ) {}
}
