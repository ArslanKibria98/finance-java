package com.ksa.financing.ledger.adapter.rest.request;

import com.ksa.financing.ledger.domain.port.in.PostSimpleAccountMovementUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for posting a two-line journal (target COA + configured offset account).
 */
public record PostSimpleAccountMovementRequest(
        String accountCode,
        UUID accountId,
        @NotNull(message = "movement is required")
        PostSimpleAccountMovementUseCase.Movement movement,
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,
        @NotNull(message = "entryDate is required")
        LocalDate entryDate,
        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 100, message = "idempotencyKey must be at most 100 characters")
        String idempotencyKey,
        @Size(max = 500)
        String description,
        @Size(max = 100)
        String referenceType,
        UUID referenceId,
        boolean returnLedgerSnapshot
) {}
