package com.ksa.financing.globalprofile.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PiiAccessTokenResponse(
    UUID tokenId,
    String accessToken,
    UUID globalUid,
    List<String> allowedFields,
    String accessPurpose,
    Instant issuedAt,
    Instant expiresAt
) {}
