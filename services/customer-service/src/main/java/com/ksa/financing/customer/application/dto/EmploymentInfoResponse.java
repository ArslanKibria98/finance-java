package com.ksa.financing.customer.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmploymentInfoResponse(
    UUID id,
    String employerName,
    String employmentType,
    String jobTitle,
    LocalDate startDate,
    BigDecimal netSalary,
    String currency,
    boolean isVerified,
    String verifiedVia,
    boolean isCurrent
) {}
