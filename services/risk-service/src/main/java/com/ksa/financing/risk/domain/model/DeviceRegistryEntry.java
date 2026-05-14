package com.ksa.financing.risk.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeviceRegistryEntry(
    UUID id,
    String deviceId,
    String deviceFingerprint,
    String nid,
    String mobileNumber,
    boolean blocked,
    String blockReason,
    String blockSource,
    String blockCode,
    UUID blockCodeId,
    int nidAssociationCount,
    int attemptCount,
    Instant firstSeenAt,
    Instant lastSeenAt,
    Instant createdAt,
    Instant updatedAt
) {}
