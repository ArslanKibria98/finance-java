package com.ksa.financing.onboarding.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitAdditionalInfoRequest(
    @NotBlank String nationalId,
    String email,
    String employerName,
    String employerCrNumber,
    String employmentType,
    String jobTitle,
    Double basicSalary,
    Double grossSalary,
    Double netSalary,
    String currency,
    String bankName,
    String bankCode,
    String iban,
    String accountHolderName,
    Boolean isPep              // true = PEP → EDD required, false/null = normal flow
) {}
