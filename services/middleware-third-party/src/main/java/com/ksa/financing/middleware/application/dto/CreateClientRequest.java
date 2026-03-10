package com.ksa.financing.middleware.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateClientRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String code,
        String description,
        String callbackUrl,
        @NotNull String environment,
        List<String> ipWhitelist
) {}
