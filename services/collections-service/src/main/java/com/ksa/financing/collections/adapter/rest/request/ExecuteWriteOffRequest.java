package com.ksa.financing.collections.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ExecuteWriteOffRequest(
        @NotNull UUID loanId,
        UUID installmentId,          // null = all eligible installments on the loan (unless invoiceId is provided)
        String invoiceId,            // optional: alternative to installmentId
        @NotBlank String reason,
        String approvalReference,
        String triggerType,          // MANUAL | AUTO_RULE (defaults MANUAL when override, else AUTO_RULE)
        Boolean override,            // true = bypass eligibility flag (admin override)
        LocalDate asOf
) {}
