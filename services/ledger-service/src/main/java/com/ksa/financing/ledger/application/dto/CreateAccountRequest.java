package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.ledger.domain.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a GL account in the Chart of Accounts.
 */
public record CreateAccountRequest(

        @NotBlank(message = "Account code is required")
        @Size(max = 50, message = "Account code must be at most 50 characters")
        String accountCode,

        @NotBlank(message = "Account name is required")
        @Size(max = 255, message = "Account name must be at most 255 characters")
        String accountName,

        @Size(max = 255, message = "Arabic account name must be at most 255 characters")
        String accountNameAr,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        String parentAccountCode,

        boolean isHeader,

        @Size(max = 34, message = "IBAN must be at most 34 characters")
        String iban
) {}
