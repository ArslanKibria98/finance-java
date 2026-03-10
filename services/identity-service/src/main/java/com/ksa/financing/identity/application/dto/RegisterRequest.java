package com.ksa.financing.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank @Email String email,
    @NotBlank String mobileNumber,
    @NotBlank String password,
    String realm,
    @NotBlank(message = "Tenant ID is required") String tenantId
) {}
