package com.ksa.financing.middleware.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEnvConfigRequest(
        @NotNull UUID apiId,
        @NotNull String environment,
        @NotBlank String baseUrl,
        String endpointPath,
        String credentials,
        String headers,
        String queryParams,
        String authType
) {}
