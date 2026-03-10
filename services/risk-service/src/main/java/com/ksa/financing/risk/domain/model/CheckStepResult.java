package com.ksa.financing.risk.domain.model;

import java.time.Instant;

public record CheckStepResult(
    String checkName,
    CheckDecision decision,
    String detail,
    int scoreContribution,
    Instant completedAt
) {}
