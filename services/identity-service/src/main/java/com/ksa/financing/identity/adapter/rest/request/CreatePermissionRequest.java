package com.ksa.financing.identity.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
    @NotBlank(message = "Permission code is required")
    @Size(max = 100, message = "Permission code must not exceed 100 characters")
    String permissionCode,

    @NotBlank(message = "Permission name is required")
    String permissionName,

    String description,

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Resource type must not exceed 50 characters")
    String resourceType,

    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    String action
) {}
