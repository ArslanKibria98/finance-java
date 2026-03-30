package com.ksa.financing.risk.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface PerformCreditCheckUseCase {

    CreditCheckResult performCreditCheck(CreditCheckCommand command);

    record CreditCheckCommand(
            UUID tenantId,
            String nationalId,
            String customerId,
            BigDecimal requestedAmount
    ) {}

    record CreditCheckResult(
            int creditScore,
            String simahGrade,
            String simahReferenceId,
            BigDecimal verifiedSalary,
            BigDecimal existingObligations,
            boolean hasActiveDefaults
    ) {}
}
