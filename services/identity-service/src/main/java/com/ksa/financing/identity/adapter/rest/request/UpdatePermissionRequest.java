package com.ksa.financing.identity.adapter.rest.request;

public record UpdatePermissionRequest(
    String permissionName,
    String description,
    Boolean active
) {}
