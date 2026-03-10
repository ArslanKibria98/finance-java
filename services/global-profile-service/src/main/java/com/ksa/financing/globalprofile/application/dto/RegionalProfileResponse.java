package com.ksa.financing.globalprofile.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegionalProfileResponse(
    UUID regionalProfileId,
    UUID globalUid,
    String countryCode,
    String regionalCifNumber,
    String regionalKycStatus,
    Instant kycVerifiedAt,
    LocalDate kycExpiryDate,
    String piiVaultRegion,
    UUID piiVaultRecordId,
    boolean active,
    Instant createdAt
) {}
