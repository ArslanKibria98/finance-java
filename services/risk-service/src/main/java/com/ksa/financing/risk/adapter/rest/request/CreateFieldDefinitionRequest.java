package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFieldDefinitionRequest(
        @NotBlank @Size(max = 100) String fieldKey,
        @NotBlank @Size(max = 255) String nameEn,
        @NotBlank @Size(max = 255) String nameAr,
        @NotBlank @Size(max = 20) String dataType,
        @NotNull Boolean active,
        @NotNull Integer sortOrder
) {}
