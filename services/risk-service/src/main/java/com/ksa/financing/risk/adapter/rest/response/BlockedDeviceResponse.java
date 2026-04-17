package com.ksa.financing.risk.adapter.rest.response;

import java.time.Instant;
import java.util.List;

public record BlockedDeviceResponse(
    String deviceId,
    String deviceFingerprint,
    boolean adminBlocked,
    String blockReason,
    String blockSource,
    String blockType,
    int totalNidAssociations,
    int totalAttempts,
    Instant firstSeenAt,
    Instant lastSeenAt,
    List<NidAssociation> nidAssociations
) {
    public record NidAssociation(
        String nidHash,
        int attemptCount,
        Instant firstSeenAt,
        Instant lastSeenAt
    ) {}
}
