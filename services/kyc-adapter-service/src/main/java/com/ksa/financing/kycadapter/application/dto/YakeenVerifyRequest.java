package com.ksa.financing.kycadapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record YakeenVerifyRequest(
    @NotBlank String nationalId,
    @NotNull LocalDate dateOfBirth
) {}
