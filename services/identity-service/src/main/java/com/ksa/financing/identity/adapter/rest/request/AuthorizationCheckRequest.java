package com.ksa.financing.identity.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record AuthorizationCheckRequest(
    @NotBlank(message = "Subject (role) is required")
    String subject,

    @NotBlank(message = "Resource (path) is required")
    String resource,

    @NotBlank(message = "Action (HTTP method) is required")
    String action
) {}
