package com.ksa.financing.identity.adapter.rest.response;

import java.util.List;
import java.util.UUID;

public record ModulePermissionsResponse(
    UUID moduleId,
    String moduleCode,
    String moduleName,
    String description,
    int displayOrder,
    List<PermissionResponse> permissions
) {}
