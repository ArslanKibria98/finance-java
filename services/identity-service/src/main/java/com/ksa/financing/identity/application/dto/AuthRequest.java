package com.ksa.financing.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank String username,
    @NotBlank String password,
    String realm
) {}
