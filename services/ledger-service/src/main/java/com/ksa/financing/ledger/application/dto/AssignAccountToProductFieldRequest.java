package com.ksa.financing.ledger.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AssignAccountToProductFieldRequest(
        @NotNull(message = "COA Field ID is required")
        UUID coaFieldId,

        @NotNull(message = "Account ID is required")
        UUID accountId,

        @NotBlank(message = "Account code is required")
        String accountCode,

        String notes
) {}
