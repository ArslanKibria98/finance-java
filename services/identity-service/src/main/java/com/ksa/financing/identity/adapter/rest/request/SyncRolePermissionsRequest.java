package com.ksa.financing.identity.adapter.rest.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SyncRolePermissionsRequest(
        @NotNull List<UUID> permissionIds
) {}
