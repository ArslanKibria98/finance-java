package com.ksa.financing.ledger.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssignCoaAccountsRequest(
        @NotEmpty List<@Valid AssignmentItem> assignments
) {
    public record AssignmentItem(
            @NotBlank @Size(max = 100) String fieldKey,
            @NotBlank @Size(max = 50) String accountCode
    ) {}
}
