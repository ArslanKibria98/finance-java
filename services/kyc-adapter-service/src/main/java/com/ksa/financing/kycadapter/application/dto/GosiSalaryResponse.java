package com.ksa.financing.kycadapter.application.dto;

import java.math.BigDecimal;

public record GosiSalaryResponse(
    String employerName,
    BigDecimal basicSalary,
    BigDecimal housingAllowance,
    BigDecimal totalSalary,
    String verificationSource
) {}
