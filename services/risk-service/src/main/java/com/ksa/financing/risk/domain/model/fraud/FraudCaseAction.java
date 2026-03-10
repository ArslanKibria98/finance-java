package com.ksa.financing.risk.domain.model.fraud;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudCaseAction(
    UUID id,
    UUID tenantId,
    UUID caseId,
    String action,
    String note,
    UUID performedBy,
    LocalDateTime performedAt
) {}
