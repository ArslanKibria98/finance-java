package com.ksa.financing.customer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddEmploymentRequest(
    @NotBlank String employerName,
    String employerCrNumber,
    String employerSector,
    String employmentType,
    String jobTitle,
    LocalDate startDate,
    BigDecimal basicSalary,
    BigDecimal housingAllowance,
    BigDecimal otherAllowances,
    BigDecimal grossSalary,
    BigDecimal deductions,
    BigDecimal netSalary,
    String salaryBankName,
    String salaryIban
) {}
