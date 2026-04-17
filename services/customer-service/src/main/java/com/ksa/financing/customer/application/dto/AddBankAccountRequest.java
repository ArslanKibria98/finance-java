package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddBankAccountRequest(
    @NotBlank String bankName,
    String bankCode,
    @NotBlank String iban,
    String accountHolderName,
    String accountType,
    Boolean isPrimary,
    Boolean isSalaryAccount
) {}
