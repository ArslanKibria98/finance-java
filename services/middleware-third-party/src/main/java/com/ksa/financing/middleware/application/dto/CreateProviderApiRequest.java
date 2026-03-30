package com.ksa.financing.middleware.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProviderApiRequest(
        @NotNull UUID providerId,
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 200) String nameEn,
        @Size(max = 200) String nameAr,
        String descriptionEn,
        String descriptionAr,
        @NotNull String httpMethod,
        @NotBlank String endpointPath,
        boolean async,
        Integer timeoutMs
) {}
