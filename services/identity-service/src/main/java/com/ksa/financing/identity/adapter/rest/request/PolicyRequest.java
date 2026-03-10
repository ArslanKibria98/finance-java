package com.ksa.financing.identity.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record PolicyRequest(
    @NotBlank(message = "Role is required")
    String role,

    @NotBlank(message = "Resource is required")
    String resource,

    @NotBlank(message = "Action is required")
    String action
) {}
