package com.ksa.financing.risk.adapter.rest.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BlockedDeviceResponse(
    String deviceId,
    String deviceFingerprint,
    boolean adminBlocked,
    String blockReason,
    String blockSource,
    String blockType,
    String blockCode,
    UUID blockCodeId,
    int totalNidAssociations,
    int totalAttempts,
    Instant firstSeenAt,
    Instant lastSeenAt,
    List<NidAssociation> nidAssociations
) {
    public record NidAssociation(
        String nid,
        String mobileNumber,
        int attemptCount,
        Instant firstSeenAt,
        Instant lastSeenAt
    ) {}
}
