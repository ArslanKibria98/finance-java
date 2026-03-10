package com.ksa.financing.identity.adapter.rest.request;

public record UpdateRoleRequest(
    String roleName,
    String roleNameAr,
    String description,
    Boolean active
) {}
