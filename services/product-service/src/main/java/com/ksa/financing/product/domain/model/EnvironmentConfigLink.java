package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record EnvironmentConfigLink(
    UUID id,
    UUID environmentConfigId,
    boolean active,
    int sortOrder
) {}
