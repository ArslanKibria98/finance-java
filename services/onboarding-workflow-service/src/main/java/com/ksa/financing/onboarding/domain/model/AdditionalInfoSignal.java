package com.ksa.financing.onboarding.domain.model;

public record AdditionalInfoSignal(
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
    DeviceInfo deviceInfo,
    boolean isPep,
    // PEP fields (only used when isPep=true)
    String sourceOfFunds,
    String estimatedNetWorth,
    String sourceOfIncome,
    String occupation
) {}
