package com.ksa.financing.middleware.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantAccessRequest(
        @NotNull UUID clientId,
        @NotNull UUID targetId,
        @NotNull String environment
) {}
