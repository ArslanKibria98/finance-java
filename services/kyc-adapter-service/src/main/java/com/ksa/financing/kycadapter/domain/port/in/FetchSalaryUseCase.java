package com.ksa.financing.kycadapter.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface FetchSalaryUseCase {
    SalaryResult fetch(FetchSalaryCommand command);

    record FetchSalaryCommand(
        UUID tenantId,
        String nationalId,
        String idempotencyKey
    ) {}

    record SalaryResult(
        String employerName,
        BigDecimal basicSalary,
        BigDecimal housingAllowance,
        BigDecimal totalSalary,
        String verificationSource
    ) {}
}
