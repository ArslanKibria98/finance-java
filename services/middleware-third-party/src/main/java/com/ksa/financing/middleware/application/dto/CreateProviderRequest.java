package com.ksa.financing.middleware.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProviderRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull String category,
        String baseUrlDev,
        String baseUrlProd,
        @NotNull String authType,
        int timeoutMs,
        int retryCount
) {}
