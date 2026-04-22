package com.ksa.financing.ledger.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for a single journal line in a posting request.
 * Exactly one of debitAmount / creditAmount must be > 0.
 */
public record JournalLineDto(

        @NotBlank(message = "Account code is required")
        String accountCode,

        @NotNull(message = "Debit amount is required")
        @DecimalMin(value = "0", message = "Debit amount cannot be negative")
        BigDecimal debitAmount,

        @NotNull(message = "Credit amount is required")
        @DecimalMin(value = "0", message = "Credit amount cannot be negative")
        BigDecimal creditAmount,

        String description
) {}
