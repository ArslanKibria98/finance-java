package com.ksa.financing.globalprofile.application.dto;

import java.time.Instant;
import java.util.UUID;

public record GlobalProfileResponse(
    UUID globalUid,
    String customerType,
    String primaryCountryCode,
    String globalKycStatus,
    String globalRiskGrade,
    boolean pepFlag,
    boolean sanctionsFlag,
    boolean fraudFlag,
    boolean active,
    String customerSegment,
    Instant createdAt,
    Instant updatedAt
) {}
