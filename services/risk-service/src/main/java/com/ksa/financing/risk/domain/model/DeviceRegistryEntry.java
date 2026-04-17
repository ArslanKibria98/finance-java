package com.ksa.financing.risk.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeviceRegistryEntry(
    UUID id,
    String deviceId,
    String deviceFingerprint,
    String nidHash,
    boolean blocked,
    String blockReason,
    String blockSource,
    int nidAssociationCount,
    int attemptCount,
    Instant firstSeenAt,
    Instant lastSeenAt,
    Instant createdAt,
    Instant updatedAt
) {}
