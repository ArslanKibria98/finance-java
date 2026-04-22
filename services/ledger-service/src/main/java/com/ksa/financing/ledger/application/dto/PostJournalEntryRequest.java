package com.ksa.financing.ledger.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for posting a balanced journal entry.
 */
public record PostJournalEntryRequest(

        @NotNull(message = "Entry date is required")
        LocalDate entryDate,

        @NotBlank(message = "Reference type is required")
        String referenceType,

        @NotNull(message = "Reference ID is required")
        UUID referenceId,

        String transactionType,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotEmpty(message = "Journal lines are required")
        @Valid
        List<JournalLineDto> lines,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100, message = "Idempotency key must be at most 100 characters")
        String idempotencyKey
) {}
