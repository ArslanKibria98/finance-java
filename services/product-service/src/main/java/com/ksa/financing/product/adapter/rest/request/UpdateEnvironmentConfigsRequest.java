package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateEnvironmentConfigsRequest(
    @Valid List<EnvironmentConfigLinkItem> configs
) {
    public record EnvironmentConfigLinkItem(
        @NotNull UUID environmentConfigId,
        boolean active,
        int sortOrder
    ) {}
}
