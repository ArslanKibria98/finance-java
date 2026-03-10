package com.ksa.financing.identity.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record RoleGroupingRequest(
    @NotBlank(message = "User is required")
    String user,

    @NotBlank(message = "Role is required")
    String role
) {}
