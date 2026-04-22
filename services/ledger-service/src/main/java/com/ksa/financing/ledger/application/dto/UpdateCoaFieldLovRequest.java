package com.ksa.financing.ledger.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCoaFieldLovRequest(
        @NotBlank @Size(max = 255) String fieldLabelEn,
        @Size(max = 255) String fieldLabelAr,
        @NotBlank @Size(max = 50) String category,
        boolean mandatoryDefault,
        @Min(0) int displayOrder
) {}
