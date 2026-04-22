package com.ksa.financing.ledger.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCoaConfigurationProfileRequest(
        @NotBlank @Size(max = 50) String productCode,
        @NotBlank @Size(max = 100) String profileName,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate effectiveFrom,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate effectiveTo
) {}
