package com.ksa.financing.customer.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AddBankAccountRequest(
    @NotBlank String bankName,
    String bankCode,
    @NotBlank String iban,
    String accountHolderName,
    String accountType,
    Boolean isPrimary,
    Boolean isSalaryAccount
) {}
