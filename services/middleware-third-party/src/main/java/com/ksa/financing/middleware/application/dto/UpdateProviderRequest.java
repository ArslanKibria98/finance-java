package com.ksa.financing.middleware.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProviderRequest(
        @NotBlank @Size(max = 200) String nameEn,
        @Size(max = 200) String nameAr,
        String descriptionEn,
        String descriptionAr,
        @NotNull String category,
        String baseUrlDev,
        String baseUrlProd,
        @NotNull String authType,
        int timeoutMs,
        int retryCount
) {}
