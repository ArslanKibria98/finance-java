package com.ksa.financing.kycadapter.application.dto;

import java.util.UUID;

public record SanctionsScreenResponse(
    UUID sessionId,
    String screeningStatus,
    boolean hit
) {}
