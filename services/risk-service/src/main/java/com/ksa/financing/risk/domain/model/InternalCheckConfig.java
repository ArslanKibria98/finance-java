package com.ksa.financing.risk.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InternalCheckConfig {
    private final UUID id;
    private final UUID tenantId;
    private final String checkName;
    private final String displayName;
    private final String description;
    private final boolean active;
    private final UUID blockCodeId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int version;
}
