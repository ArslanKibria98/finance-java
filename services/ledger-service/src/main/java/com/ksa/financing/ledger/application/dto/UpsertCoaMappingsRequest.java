package com.ksa.financing.ledger.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertCoaMappingsRequest(
        @NotEmpty List<@Valid MappingItem> mappings
) {
    public record MappingItem(
            @NotBlank @Size(max = 100) String fieldKey,
            @Size(max = 50) String accountCode,
            Boolean mandatoryOverride,
            String notes
    ) {}
}
